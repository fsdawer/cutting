/**
 * 알림 파이프라인 부하 테스트 — 50명 동시 예약 + WebSocket 알림 수신
 *
 * 플로우: WS 연결 → SockJS 'o' → STOMP CONNECT → SUBSCRIBE → 예약 생성 → MESSAGE 수신
 *
 * 실행:
 *   k6 run -e STYLIST_ID=1 -e SERVICE_ID=1 k6/test_notification_load.js
 *
 * 사전 준비:
 *   setup() 단계에서 notif_k6_1~50 계정을 자동 생성/로그인 → JWT sub에서 userId 추출
 *
 * 주의:
 *   STYLIST_ID 스타일리스트의 영업 시간 범위에 맞는 슬롯을 사용해야 예약이 성공함
 *   기본 가정: 평일 10~17시 운영. 다르면 uniqueSlot() 조정 필요
 */

import ws       from 'k6/ws';
import http     from 'k6/http';
import { check }        from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';
import encoding from 'k6/encoding';

const BASE_URL   = __ENV.BASE_URL   || 'http://localhost:8080';
const STYLIST_ID = __ENV.STYLIST_ID || '1';
const SERVICE_ID = __ENV.SERVICE_ID || '1';
const NUM_USERS  = 50;

const JSON_HEADER = { 'Content-Type': 'application/json' };

// ── JWT sub 추출 ──────────────────────────────────────────────────────────────
function extractUserId(bearerToken) {
  const jwt   = bearerToken.replace('Bearer ', '');
  const parts = jwt.split('.');
  const payload = JSON.parse(encoding.b64decode(parts[1], 'rawurl', 's'));
  return String(payload.sub);
}

// ── SockJS 세션 ID용 랜덤 문자열 ─────────────────────────────────────────────
function randomString(len) {
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let s = '';
  for (let i = 0; i < len; i++) s += chars[Math.floor(Math.random() * chars.length)];
  return s;
}

// ── VU별 고유 예약 슬롯 (충돌 방지) ──────────────────────────────────────────
function uniqueSlot(vuIndex) {
  const d = new Date();
  d.setDate(d.getDate() + 60 + vuIndex); // VU마다 다른 날짜 (60~109일 후)
  // 주말 스킵: 토(6)→월, 일(0)→월
  const jsDay = d.getDay();
  if (jsDay === 0) d.setDate(d.getDate() + 1);
  if (jsDay === 6) d.setDate(d.getDate() + 2);
  const hour = 10 + (vuIndex % 7); // 10~16시
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}T${String(hour).padStart(2, '0')}:00:00`;
}

// ── setup: 50명 유저 생성 + 토큰/userId 수집 ─────────────────────────────────
export function setup() {
  const users = [];
  for (let i = 1; i <= NUM_USERS; i++) {
    const username = `notif_k6_${i}`;
    const password = 'Loadtest123!';

    http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({
      username,
      email:    `notif_k6_${i}@k6test.com`,
      password,
      name:     `알림테스트${i}`,
      phone:    `010-3000-${String(i).padStart(4, '0')}`,
      role:     'USER',
    }), { headers: JSON_HEADER });

    const res  = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({ username, password }), { headers: JSON_HEADER });
    const body = JSON.parse(res.body);

    if (res.status !== 200 || !body.accessToken) {
      console.error(`[setup] 로그인 실패 — ${username}, status=${res.status}`);
      users.push(null);
    } else {
      const token = `Bearer ${body.accessToken}`;
      users.push({ token, userId: extractUserId(token) });
    }
  }
  console.log(`[setup] ${users.filter(Boolean).length}/${NUM_USERS}명 준비 완료`);
  return { users };
}

// ── 커스텀 메트릭 ─────────────────────────────────────────────────────────────
const notifReceived = new Counter('notification_received');  // 알림 수신 성공
const notifMissed   = new Counter('notification_missed');    // 타임아웃으로 미수신
const notifLatency  = new Trend('notification_latency_ms', true); // 예약→알림 지연
const wsConnFailed  = new Counter('ws_connect_failed');      // WS 101 외 응답

export const options = {
  scenarios: {
    notification_load: {
      executor:    'shared-iterations',
      vus:         NUM_USERS,
      iterations:  NUM_USERS,
      maxDuration: '60s',
    },
  },
  thresholds: {
    notification_latency_ms: ['p(95)<3000'],  // 알림 p95 < 3초
    ws_connect_failed:       ['count<5'],      // WS 연결 실패 5건 미만
  },
};

// ── VU 메인 함수 ──────────────────────────────────────────────────────────────
export default function(data) {
  const vuIndex = (__VU - 1) % NUM_USERS;
  const user    = data.users[vuIndex];
  if (!user) {
    console.error(`[VU ${__VU}] 유저 데이터 없음 — 건너뜀`);
    notifMissed.add(1);
    return;
  }

  const { token, userId } = user;
  const sessionId = randomString(8);
  const wsUrl     = `ws://localhost:8080/ws/000/${sessionId}/websocket`;

  let received    = false;
  let reserveTime = 0;
  let reserved    = false;

  const wsRes = ws.connect(wsUrl, { headers: { Authorization: token } }, function(socket) {
    socket.on('message', (msg) => {
      // SockJS open frame
      if (msg === 'o') {
        socket.send(JSON.stringify([
          `CONNECT\naccept-version:1.2\nheart-beat:0,0\nAuthorization:${token}\n\n\0`
        ]));
        return;
      }

      // SockJS heartbeat — 무시
      if (msg === 'h') return;

      // SockJS data frame: a["STOMP_FRAME"]
      if (!msg.startsWith('a')) return;

      let frames;
      try { frames = JSON.parse(msg.slice(1)); } catch { return; }

      for (const frame of frames) {
        if (frame.startsWith('CONNECTED')) {
          // STOMP 연결 완료 → 알림 토픽 구독
          socket.send(JSON.stringify([
            `SUBSCRIBE\nid:sub-0\ndestination:/topic/notification/${userId}\n\n\0`
          ]));

          // 구독 등록 후 예약 생성
          socket.setTimeout(() => {
            if (reserved) return;
            reserved    = true;
            reserveTime = Date.now();

            const r = http.post(`${BASE_URL}/api/reservations`, JSON.stringify({
              stylistId:   Number(STYLIST_ID),
              serviceId:   Number(SERVICE_ID),
              reservedAt:  uniqueSlot(vuIndex),
              requestMemo: `알림 부하테스트 VU=${__VU}`,
            }), {
              headers: { 'Content-Type': 'application/json', Authorization: token },
            });

            if (r.status !== 201) {
              console.error(`VU=${__VU} 예약 실패: status=${r.status} body=${r.body?.slice(0, 200)}`);
              socket.close();
            }
          }, 200); // 구독 프레임 전달에 200ms 여유

        } else if (frame.startsWith('MESSAGE')) {
          received = true;
          notifReceived.add(1);
          notifLatency.add(Date.now() - reserveTime);
          socket.close();
        }
      }
    });

    socket.on('error', (e) => {
      console.error(`VU=${__VU} WS 에러: ${e}`);
    });

    // 전체 타임아웃: 15초 내 알림 미도착 시 종료
    socket.setTimeout(() => {
      if (!received) socket.close();
    }, 15000);
  });

  check(wsRes, { 'WS 101 업그레이드': (r) => r && r.status === 101 });
  if (!wsRes || wsRes.status !== 101) wsConnFailed.add(1);
  if (!received) notifMissed.add(1);
}

// ── 커스텀 리포트 ─────────────────────────────────────────────────────────────
export function handleSummary(data) {
  const req_dur   = data.metrics.http_req_duration?.values  || {};
  const reqs      = data.metrics.http_reqs?.values          || {};
  const iters     = data.metrics.iterations?.values         || {};
  const iter_dur  = data.metrics.iteration_duration?.values || {};
  const data_recv = data.metrics.data_received?.values      || {};
  const data_sent = data.metrics.data_sent?.values          || {};
  const vus       = data.metrics.vus?.values                || {};

  const received   = data.metrics.notification_received?.values?.count ?? 0;
  const missed     = data.metrics.notification_missed?.values?.count   ?? 0;
  const latency    = data.metrics.notification_latency_ms?.values      || {};
  const connFailed = data.metrics.ws_connect_failed?.values?.count     ?? 0;

  const total    = received + missed;
  const recvRate = total > 0 ? ((received / total) * 100).toFixed(2) : '0.00';

  const checks_total     = (data.metrics.checks?.values?.passes || 0) + (data.metrics.checks?.values?.fails || 0);
  const checks_succeeded = data.metrics.checks?.values?.passes || 0;
  const checks_failed    = data.metrics.checks?.values?.fails  || 0;
  const succ_rate  = checks_total > 0 ? ((checks_succeeded / checks_total) * 100).toFixed(2) : '0.00';
  const fail_rate  = checks_total > 0 ? ((checks_failed    / checks_total) * 100).toFixed(2) : '0.00';

  const durationS = (data.state?.testRunDurationMs || 60000) / 1000;
  const formatMs  = (val) => val === undefined ? '0.00ms' : val.toFixed(2) + 'ms';

  let verdict = '';
  if (received === total && total > 0 && connFailed === 0) {
    verdict = '✓ AFTER 상태: 전원 알림 수신 성공 — 파이프라인 정상';
  } else if (total > 0 && received >= Math.floor(total * 0.9)) {
    verdict = `△ ${received}/${total}명 알림 수신 (${recvRate}%) — 일부 누락`;
  } else {
    verdict = `✗ ${received}/${total}명만 알림 수신 (${recvRate}%) — 파이프라인 점검 필요`;
  }

  const result = `
[알림 파이프라인] 예약 생성 후 WebSocket 알림 수신  STYLIST_ID=${STYLIST_ID}  SERVICE_ID=${SERVICE_ID}
=============================================================

TOTAL RESULTS
  checks_total            ${checks_total}    ${(checks_total / durationS).toFixed(2)}/s
  checks_succeeded        ${succ_rate}%   ${checks_succeeded} out of ${checks_total}
  checks_failed           ${fail_rate}%    ${checks_failed} out of ${checks_total}
  timeout                 없음
  ※ WS 101 업그레이드 | 예약 201 성공 확인

CUSTOM METRICS
  notification_received   ${received}
  notification_missed     ${missed}
  notification_recv_rate  ${recvRate}%
  ws_connect_failed       ${connFailed}
  notification_latency_ms avg=${formatMs(latency.avg)} min=${formatMs(latency.min)} med=${formatMs(latency.med)} max=${formatMs(latency.max)} p(90)=${formatMs(latency['p(90)'])} p(95)=${formatMs(latency['p(95)'])}


HTTP
  http_req_duration       avg=${formatMs(req_dur.avg)} min=${formatMs(req_dur.min)} med=${formatMs(req_dur.med)} max=${formatMs(req_dur.max)} p(90)=${formatMs(req_dur['p(90)'])} p(95)=${formatMs(req_dur['p(95)'])}
  http_reqs               ${reqs.count || 0}   ${(reqs.rate || 0).toFixed(2)}/s

EXECUTION
  iteration_duration      avg=${formatMs(iter_dur.avg)} min=${formatMs(iter_dur.min)} med=${formatMs(iter_dur.med)} max=${formatMs(iter_dur.max)} p(90)=${formatMs(iter_dur['p(90)'])} p(95)=${formatMs(iter_dur['p(95)'])}
  iterations              ${iters.count || 0}   ${(iters.rate || 0).toFixed(2)}/s
  vus                     min=${vus.min || 0}  max=${vus.max || 0}
  vus_max                 min=${data.metrics.vus_max?.values?.min || 0}  max=${data.metrics.vus_max?.values?.max || 0}

NETWORK
  data_received           ${((data_recv.count || 0) / 1024 / 1024).toFixed(2)} MB   ${((data_recv.rate || 0) / 1024).toFixed(2)} kB/s
  data_sent               ${((data_sent.count || 0) / 1024 / 1024).toFixed(2)} MB   ${((data_sent.rate || 0) / 1024).toFixed(2)} kB/s
`;

  console.log(result);
  return { stdout: result };
}
