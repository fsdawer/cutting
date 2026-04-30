<template>
  <main class="chat-page">
    <!-- 로딩 -->
    <div v-if="loading" class="loading-full">
      <div class="spinner"></div>
    </div>

    <div v-else class="chat-layout">
      <!-- 헤더 -->
      <div class="chat-header">
        <button class="back-btn" @click="$router.back()">←</button>
        <div class="ch-avatar-box">{{ partnerInitial }}</div>
        <div class="ch-info">
          <p class="ch-name">{{ partnerName }}</p>
          <p class="ch-sub" v-if="salonName">{{ salonName }}</p>
        </div>
      </div>

      <!-- 메시지 영역 -->
      <div class="messages-area" ref="msgArea">
        <p v-if="messages.length === 0" class="empty-msg">아직 대화가 없습니다. 첫 메시지를 보내보세요!</p>

        <div
          v-for="msg in messages"
          :key="msg.id"
          class="msg-row"
          :class="{ mine: msg.senderId === myId }"
        >
          <div v-if="msg.senderId !== myId" class="msg-avatar-box">{{ partnerInitial }}</div>
          <div class="msg-bubble-wrap">
            <div class="msg-bubble" :class="{ mine: msg.senderId === myId, pending: msg._pending }">
              <img v-if="msg.imageUrl" :src="msg.imageUrl" class="msg-image" />
              <p v-else>{{ msg.content }}</p>
            </div>
            <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
          </div>
        </div>

        <!-- 타이핑 인디케이터 -->
        <div v-if="partnerTyping" class="msg-row">
          <div class="msg-avatar-box">{{ partnerInitial }}</div>
          <div class="typing-indicator">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>

      <!-- 입력 영역 -->
      <div class="chat-input-area">
        <input
          v-model="inputText"
          class="chat-input"
          placeholder="메시지를 입력하세요..."
          @keyup.enter="sendMessage"
        />
        <button class="send-btn" :disabled="!inputText.trim() || !connected" @click="sendMessage">
          전송
        </button>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { chatApi } from '@/api/chat'
import { useAuthStore } from '@/stores/authStore'

const route  = useRoute()
const router = useRouter()
const auth   = useAuthStore()

const roomId = route.params.roomId
const myId   = computed(() => auth.user?.id)

const room         = ref(null)
const messages     = ref([])
const loading      = ref(true)
const inputText    = ref('')
const connected    = ref(false)
const partnerTyping = ref(false)
const msgArea      = ref(null)

let stompClient = null

// 상대방 정보 (내가 고객이면 → 미용사가 상대, 내가 미용사면 → 고객이 상대)
const partnerName = computed(() => {
  if (!room.value) return ''
  return myId.value === room.value.userId ? room.value.stylistUserName : room.value.userName
})
const salonName = computed(() => {
  if (!room.value || myId.value !== room.value.userId) return ''
  return room.value.salonName || ''
})
const partnerInitial = computed(() => partnerName.value ? partnerName.value.charAt(0) : '?')

onMounted(async () => {
  // REST API로 방 정보와 메시지를 각각 로드 — 하나 실패해도 다른 것은 유지
  try {
    const roomRes  = await chatApi.getRoom(roomId)
    room.value     = roomRes.data
  } catch (e) {
    console.error('채팅방 정보 로드 실패', e)
  }

  try {
    const msgRes   = await chatApi.getMessages(roomId)
    messages.value = msgRes.data
    await scrollToBottom()
    chatApi.markAsRead(roomId).catch(() => {})
  } catch (e) {
    console.error('메시지 로드 실패', e)
  }

  loading.value = false

  // STOMP WebSocket 연결
  try {
    stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: {
        Authorization: `Bearer ${auth.token}`,
      },
      reconnectDelay: 3000,
      onConnect: () => {
        connected.value = true
        stompClient.subscribe(`/topic/chat/${roomId}`, async (frame) => {
          const msg = JSON.parse(frame.body)
          if (msg.senderId === myId.value) {
            const pendingIdx = messages.value.findIndex(m => m._pending && m.content === msg.content)
            if (pendingIdx !== -1) {
              messages.value.splice(pendingIdx, 1, msg)
            }
          } else {
            messages.value.push(msg)
            await scrollToBottom()
            chatApi.markAsRead(roomId).catch(() => {})
          }
        })
      },
      onDisconnect: () => { connected.value = false },
      onStompError: (frame) => {
        console.error('STOMP 에러', frame)
        connected.value = false
      },
    })
    stompClient.activate()
  } catch (e) {
    console.error('WebSocket 연결 초기화 실패', e)
  }
})

onUnmounted(() => {
  stompClient?.deactivate()
})

async function sendMessage() {
  const content = inputText.value.trim()
  if (!content || !connected.value) return

  // 낙관적 업데이트 — 서버 확정 전에 화면에 먼저 표시 (_pending: true로 구분)
  messages.value.push({
    id:        `local-${Date.now()}`,
    senderId:  myId.value,
    content,
    createdAt: new Date().toISOString(),
    _pending:  true,
  })
  inputText.value = ''
  await scrollToBottom()

  stompClient.publish({
    destination: `/app/chat/${roomId}`,
    body: JSON.stringify({
      roomId:   Number(roomId),
      senderId: myId.value,
      content,
    }),
  })
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function scrollToBottom() {
  await nextTick()
  if (msgArea.value) msgArea.value.scrollTop = msgArea.value.scrollHeight
}
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 68px);
  display: flex;
  flex-direction: column;
}
.loading-full {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.chat-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 720px;
  margin: 0 auto;
  width: 100%;
}

/* 헤더 */
.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}
.back-btn {
  background: none;
  border: none;
  font-size: 20px;
  color: var(--text-sub);
  cursor: pointer;
  padding: 4px;
}
.back-btn:hover { color: var(--text); }
.ch-avatar-box {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--primary-light);
  color: var(--primary);
  font-size: 17px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.ch-info { flex: 1; }
.ch-name { font-size: 15px; font-weight: 600; color: var(--text); }
.ch-sub  { font-size: 12px; color: var(--text-muted); margin-top: 2px; }

/* 메시지 영역 */
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: var(--bg);
}
.empty-msg {
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
  margin-top: 40px;
}

.msg-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}
.msg-row.mine { flex-direction: row-reverse; }

.msg-avatar-box {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--primary-light);
  color: var(--primary);
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.msg-bubble-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 65%;
}
.msg-row.mine .msg-bubble-wrap { align-items: flex-end; }

.msg-bubble {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px 16px 16px 4px;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text);
  word-break: break-word;
}
.msg-bubble.mine {
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: 16px 16px 4px 16px;
}
/* 서버 확정 전 낙관적 메시지 — 전송 중임을 시각적으로 표시 */
.msg-bubble.pending { opacity: 0.55; }
.msg-bubble p { margin: 0; }
.msg-image {
  width: 180px;
  height: 180px;
  object-fit: cover;
  border-radius: 12px;
  display: block;
}
.msg-time {
  font-size: 11px;
  color: var(--text-muted);
}

/* 타이핑 인디케이터 */
.typing-indicator {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px 16px 16px 4px;
  padding: 12px 16px;
  display: flex;
  gap: 4px;
  align-items: center;
}
.typing-indicator span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--text-muted);
  animation: bounce 1.2s infinite;
}
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30%            { transform: translateY(-6px); }
}

/* 입력 영역 */
.chat-input-area {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 12px 16px;
  background: var(--bg-card);
  border-top: 1px solid var(--border);
  flex-shrink: 0;
}
.chat-input {
  flex: 1;
  background: var(--bg);
  border: 1.5px solid var(--border);
  border-radius: var(--radius-full);
  padding: 10px 18px;
  color: var(--text);
  font-size: 14px;
  transition: var(--transition);
}
.chat-input:focus { border-color: var(--primary); outline: none; }
.chat-input::placeholder { color: var(--text-muted); }
.send-btn {
  padding: 10px 22px;
  border-radius: var(--radius-full);
  background: var(--primary);
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  border: none;
  cursor: pointer;
  transition: var(--transition);
  flex-shrink: 0;
}
.send-btn:hover:not(:disabled) { background: var(--primary-dark); }
.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
