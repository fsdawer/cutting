<template>
  <main class="page">
    <div class="container">
      <div class="page-header">
        <div>
          <h1 class="section-title">프로필 관리</h1>
          <p class="section-subtitle">미용사 프로필, 서비스, 영업시간을 관리하세요</p>
        </div>
        <RouterLink to="/stylist/reservations" class="btn btn-ghost btn-sm">예약 관리 →</RouterLink>
      </div>

      <div class="manage-layout">
        <!-- 좌측 네비 -->
        <aside class="manage-nav">
          <button v-for="sec in sections" :key="sec.id" class="nav-item" :class="{ active: activeSection === sec.id }" @click="activeSection = sec.id">
            {{ sec.label }}
          </button>
        </aside>

        <!-- 콘텐츠 -->
        <div class="manage-content">

          <!-- 기본 정보 -->
          <div v-if="activeSection === 'basic'" class="card">
            <h2 class="card-section-title">기본 정보</h2>
            <div class="form-layout">
              <div class="photo-upload" @click="$refs.photoInput.click()">
                <input ref="photoInput" type="file" accept="image/*" style="display:none" />
                <img :src="profile.profileImg || `https://i.pravatar.cc/120?img=47`" class="current-photo" />
                <div class="photo-overlay">변경</div>
              </div>
              <div class="form-fields">
                <div class="form-group">
                  <label class="form-label">미용실 이름</label>
                  <input v-model="profile.salonName" class="form-input" placeholder="예: Studio D" />
                </div>
                <div class="form-group">
                  <label class="form-label">미용실 주소</label>
                  <input v-model="profile.location" class="form-input" placeholder="예: 서울 강남구 청담동" />
                </div>
                <div class="form-group">
                  <label class="form-label">미용실 전화번호</label>
                  <input v-model="profile.salonPhone" type="tel" class="form-input" placeholder="02-1234-5678" />
                </div>
                <div class="form-group">
                  <label class="form-label">경력 (년)</label>
                  <input v-model.number="profile.experience" type="number" class="form-input" min="0" />
                </div>
              </div>
            </div>
            <div class="form-group" style="margin-top:16px">
              <label class="form-label">미용실 소개</label>
              <textarea v-model="profile.salonDescription" class="form-input" rows="2" placeholder="미용실을 소개해 주세요"></textarea>
            </div>
            <div class="form-group" style="margin-top:12px">
              <label class="form-label">자기 소개</label>
              <textarea v-model="profile.bio" class="form-input" rows="3" placeholder="스타일리스트 소개를 입력해 주세요"></textarea>
            </div>
            <p v-if="basicMsg" :class="basicSuccess ? 'msg-success' : 'msg-error'">{{ basicMsg }}</p>
            <div class="save-row">
              <button class="btn btn-primary" :disabled="basicSaving" @click="saveBasic">
                <span v-if="basicSaving" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
                <span v-else>저장</span>
              </button>
            </div>
          </div>

          <!-- 영업시간 -->
          <div v-if="activeSection === 'hours'" class="card">
            <h2 class="card-section-title">영업시간</h2>
            <div class="hours-table">
              <div class="hours-header">
                <span>요일</span><span>영업 여부</span><span>시작</span><span>종료</span>
              </div>
              <div v-for="h in hoursForm" :key="h.dayIndex" class="hour-row">
                <span class="hour-day" :class="{ weekend: h.dayIndex === 0 || h.dayIndex === 6 }">{{ h.day }}</span>
                <label class="toggle-label">
                  <input type="checkbox" v-model="h.open" class="toggle-check" />
                  <span class="toggle-pill" :class="{ on: h.open }"></span>
                  <span class="toggle-text">{{ h.open ? '영업' : '휴무' }}</span>
                </label>
                <input v-if="h.open" v-model="h.start" type="time" class="form-input time-input" />
                <span v-else class="no-time">—</span>
                <input v-if="h.open" v-model="h.end" type="time" class="form-input time-input" />
                <span v-else class="no-time">—</span>
              </div>
            </div>
            <p v-if="hoursMsg" :class="hoursSuccess ? 'msg-success' : 'msg-error'">{{ hoursMsg }}</p>
            <div class="save-row">
              <button class="btn btn-primary" :disabled="hoursSaving" @click="saveHours">
                <span v-if="hoursSaving" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
                <span v-else>저장</span>
              </button>
            </div>
          </div>

          <!-- 서비스 -->
          <div v-if="activeSection === 'services'" class="card">
            <h2 class="card-section-title">서비스 메뉴</h2>
            <p class="manage-hint">카테고리별로 서비스와 가격을 등록하면 고객에게 표시됩니다.</p>
            <div class="svc-list">
              <div v-for="(svc, i) in services" :key="i" class="svc-row">
                <div class="svc-row-main">
                  <select v-model="svc.category" class="form-input svc-select">
                    <option v-for="c in serviceCategories" :key="c" :value="c">{{ c }}</option>
                  </select>
                  <input v-model="svc.name" class="form-input svc-name" placeholder="서비스명" />
                  <div class="svc-num-wrap">
                    <input v-model.number="svc.durationMinutes" type="number" class="form-input svc-num" placeholder="60" min="1" />
                    <span class="svc-unit">분</span>
                  </div>
                  <div class="svc-num-wrap">
                    <input v-model.number="svc.price" type="number" class="form-input svc-num" placeholder="30000" min="0" />
                    <span class="svc-unit">원</span>
                  </div>
                  <button class="del-btn" @click="removeService(i, svc)">×</button>
                </div>
                <input v-model="svc.description" class="form-input svc-desc" placeholder="서비스 설명 (선택)" />
              </div>
            </div>
            <button class="btn btn-ghost add-btn" @click="addService">+ 서비스 추가</button>
            <p v-if="svcMsg" :class="svcSuccess ? 'msg-success' : 'msg-error'" style="margin-top:12px">{{ svcMsg }}</p>
            <div class="save-row">
              <button class="btn btn-primary" :disabled="svcSaving" @click="saveServices">
                <span v-if="svcSaving" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
                <span v-else>저장</span>
              </button>
            </div>
          </div>

        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { stylistApi } from '@/api/stylist'

const activeSection = ref('basic')
const sections = [
  { id: 'basic',     label: '기본 정보' },
  { id: 'hours',     label: '영업시간' },
  { id: 'services',  label: '서비스 메뉴' },
]
const serviceCategories = ['커트', '일반펌', '열펌', '염색', '클리닉', '기타']

const profile = ref({ salonName: '', location: '', salonPhone: '', salonDescription: '', experience: 0, bio: '', profileImg: '' })
const basicMsg = ref(''); const basicSuccess = ref(false); const basicSaving = ref(false)

const hoursForm = ref(
  ['일','월','화','수','목','금','토'].map((day, i) => ({ day, dayIndex: i, open: i !== 0, start: '10:00', end: '20:00' }))
)
const hoursMsg = ref(''); const hoursSuccess = ref(false); const hoursSaving = ref(false)

const services = ref([])
const svcMsg = ref(''); const svcSuccess = ref(false); const svcSaving = ref(false)
const deletedServiceIds = ref([])

onMounted(async () => {
  try {
    const res = await stylistApi.getMyProfile()
    const d = res.data
    profile.value = {
      salonName: d.salonName || '', location: d.location || '', salonPhone: d.salonPhone || '',
      salonDescription: d.salonDescription || '', experience: d.experience || 0,
      bio: d.bio || '', profileImg: d.profileImg || '',
    }
    services.value = (d.services || []).map(s => ({ ...s, isNew: false }))
    if (d.workingHours?.length) {
      d.workingHours.forEach(wh => {
        const h = hoursForm.value.find(f => f.dayIndex === wh.dayOfWeek)
        if (h) { h.open = !wh.isDayOff; h.start = wh.openTime?.slice(0,5) || '10:00'; h.end = wh.closeTime?.slice(0,5) || '20:00' }
      })
    }
  } catch {}
})

async function saveBasic() {
  basicSaving.value = true; basicMsg.value = ''
  try {
    await stylistApi.updateProfile({ salonName: profile.value.salonName, location: profile.value.location, salonPhone: profile.value.salonPhone || null, salonDescription: profile.value.salonDescription || null, bio: profile.value.bio, experience: Number(profile.value.experience) || 0 })
    basicMsg.value = '저장되었습니다.'; basicSuccess.value = true
  } catch (e) { basicMsg.value = e.response?.data?.message || '저장 실패'; basicSuccess.value = false }
  finally { basicSaving.value = false }
}

async function saveHours() {
  hoursSaving.value = true; hoursMsg.value = ''
  try {
    for (const h of hoursForm.value) {
      await stylistApi.updateHours({ dayOfWeek: h.dayIndex, openTime: h.open ? `${h.start}:00` : null, closeTime: h.open ? `${h.end}:00` : null, isDayOff: !h.open })
    }
    hoursMsg.value = '저장되었습니다.'; hoursSuccess.value = true
  } catch (e) { hoursMsg.value = e.response?.data?.message || '저장 실패'; hoursSuccess.value = false }
  finally { hoursSaving.value = false }
}

async function saveServices() {
  svcSaving.value = true; svcMsg.value = ''
  try {
    for (const svc of services.value) {
      const payload = { name: svc.name, category: svc.category || '기타', durationMinutes: svc.durationMinutes || 60, price: svc.price || 0, description: svc.description || '' }
      if (svc.isNew && svc.name) { const r = await stylistApi.addService(payload); svc.id = r.data.id; svc.isNew = false }
      else if (!svc.isNew && svc.id && svc.name) { await stylistApi.updateService(svc.id, payload) }
    }
    for (const id of deletedServiceIds.value) { await stylistApi.deleteService(id) }
    deletedServiceIds.value = []
    svcMsg.value = '저장되었습니다.'; svcSuccess.value = true
  } catch (e) { svcMsg.value = e.response?.data?.message || '저장 실패'; svcSuccess.value = false }
  finally { svcSaving.value = false }
}

function addService() { services.value.push({ name: '', category: '커트', durationMinutes: 60, price: 0, description: '', isNew: true }) }
function removeService(idx, svc) { if (svc.id && !svc.isNew) deletedServiceIds.value.push(svc.id); services.value.splice(idx, 1) }

</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; gap: 16px; }
.manage-layout { display: grid; grid-template-columns: 180px 1fr; gap: 20px; align-items: start; }

/* Nav */
.manage-nav { display: flex; flex-direction: column; gap: 2px; position: sticky; top: 76px; }
.nav-item {
  padding: 10px 14px; border-radius: var(--radius-sm); border: none; background: transparent;
  font-size: 14px; font-weight: 500; color: var(--text-muted); cursor: pointer; text-align: left; transition: var(--transition);
}
.nav-item:hover { background: var(--bg-surface); color: var(--text-sub); }
.nav-item.active { background: var(--bg-surface); color: var(--primary); font-weight: 700; border-left: 3px solid var(--primary); padding-left: 11px; }

.card-section-title { font-size: 13px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.06em; margin-bottom: 20px; }
.manage-hint { font-size: 13px; color: var(--text-muted); margin-bottom: 16px; }

/* Photo */
.form-layout { display: grid; grid-template-columns: 100px 1fr; gap: 20px; align-items: start; }
.form-fields { display: flex; flex-direction: column; gap: 12px; }
.photo-upload { position: relative; width: 100px; height: 100px; border-radius: 50%; overflow: hidden; cursor: pointer; border: 2px solid var(--border); }
.current-photo { width: 100%; height: 100%; object-fit: cover; }
.photo-overlay {
  position: absolute; inset: 0; background: rgba(0,0,0,0.45);
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 600; color: #fff; opacity: 0; transition: var(--transition);
}
.photo-upload:hover .photo-overlay { opacity: 1; }

/* Hours */
.hours-table { display: flex; flex-direction: column; gap: 4px; margin-bottom: 20px; }
.hours-header {
  display: grid; grid-template-columns: 48px 100px 1fr 1fr;
  gap: 12px; padding: 8px 0;
  font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.06em;
}
.hour-row { display: grid; grid-template-columns: 48px 100px 1fr 1fr; gap: 12px; align-items: center; padding: 4px 0; }
.hour-day { font-size: 14px; font-weight: 600; }
.hour-day.weekend { color: var(--danger); }
.toggle-label { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.toggle-check { display: none; }
.toggle-pill {
  width: 36px; height: 20px; border-radius: 10px; background: var(--border-strong);
  position: relative; transition: var(--transition); flex-shrink: 0;
}
.toggle-pill::after {
  content: ''; position: absolute; top: 3px; left: 3px;
  width: 14px; height: 14px; border-radius: 50%; background: #fff; transition: var(--transition);
}
.toggle-pill.on { background: var(--success); }
.toggle-pill.on::after { left: calc(100% - 17px); }
.toggle-text { font-size: 13px; color: var(--text-muted); }
.time-input { padding: 8px 10px; font-size: 13px; }
.no-time { font-size: 14px; color: var(--border-strong); }

/* Services */
.svc-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 12px; }
.svc-row { display: flex; flex-direction: column; gap: 8px; padding: 14px; background: var(--bg-surface); border-radius: var(--radius-md); border: 1px solid var(--border); }
.svc-row-main { display: grid; grid-template-columns: 100px 1fr 110px 130px 32px; gap: 8px; align-items: center; }
.svc-select { font-size: 13px; }
.svc-name { font-size: 14px; }
.svc-num-wrap { display: flex; align-items: center; gap: 4px; }
.svc-num { flex: 1; min-width: 0; padding: 8px 10px; font-size: 13px; }
.svc-unit { font-size: 12px; color: var(--text-muted); white-space: nowrap; }
.svc-desc { font-size: 13px; padding: 8px 12px; }
.del-btn {
  width: 32px; height: 32px; border-radius: var(--radius-sm); border: 1px solid var(--danger);
  background: transparent; color: var(--danger); font-size: 16px;
  display: flex; align-items: center; justify-content: center; cursor: pointer; transition: var(--transition); flex-shrink: 0;
}
.del-btn:hover { background: var(--danger-light); }
.add-btn { width: 100%; }

/* Bottom actions */
.save-row { display: flex; justify-content: flex-end; margin-top: 24px; padding-top: 20px; border-top: 1px solid var(--border); }
.msg-success { color: var(--success); font-size: 13px; margin-top: 8px; }
.msg-error   { color: var(--danger); font-size: 13px; margin-top: 8px; }


@media (max-width: 768px) {
  .manage-layout { grid-template-columns: 1fr; }
  .manage-nav { flex-direction: row; position: static; overflow-x: auto; }
  .form-layout { grid-template-columns: 1fr; }
  .hours-header { display: none; }
  .hour-row { grid-template-columns: 40px 90px 1fr 1fr; gap: 8px; }
  .svc-row-main { grid-template-columns: 90px 1fr; row-gap: 8px; }
  .svc-num-wrap { grid-column: 1; }
}
</style>
