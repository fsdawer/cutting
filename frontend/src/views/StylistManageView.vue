<template>
  <main class="page">
    <div class="container">
      <h1 class="section-title">내 프로필 관리</h1>
      <p class="section-subtitle">미용사 프로필 정보를 등록하고 관리하세요</p>

      <div class="manage-layout">
        <!-- Left Nav -->
        <aside class="manage-nav">
          <button
            v-for="sec in sections"
            :key="sec.id"
            class="nav-item"
            :class="{ active: activeSection === sec.id }"
            @click="activeSection = sec.id"
          >
            <span>{{ sec.icon }}</span>
            <span>{{ sec.label }}</span>
          </button>
        </aside>

        <!-- Content Area -->
        <div class="manage-content">
          <!-- 기본 정보 -->
          <div v-if="activeSection === 'basic'" class="card">
            <h2 class="manage-title">기본 정보</h2>
            <div class="form-grid">
              <div class="photo-upload" @click="$refs.photoInput.click()">
                <input ref="photoInput" type="file" accept="image/*" style="display:none" />
                <img :src="profile.profileImg || 'https://i.pravatar.cc/120?img=47'" class="current-photo" />
                <div class="photo-overlay">사진 변경</div>
              </div>
              <div class="form-fields">
                <div class="form-group">
                  <label class="form-label">미용실 이름</label>
                  <input v-model="profile.salonName" type="text" class="form-input" placeholder="예: D.E Studio" />
                </div>
                <div class="form-group">
                  <label class="form-label">미용실 주소</label>
                  <input v-model="profile.location" type="text" class="form-input" placeholder="예: 강남구 청담동" />
                </div>
                <div class="form-group">
                  <label class="form-label">미용실 전화번호</label>
                  <input v-model="profile.salonPhone" type="tel" class="form-input" placeholder="예: 02-1234-5678" />
                </div>
                <div class="form-group">
                  <label class="form-label">경력 (년)</label>
                  <input v-model.number="profile.experience" type="number" class="form-input" min="0" placeholder="예: 8" />
                </div>
              </div>
            </div>
            <div class="form-group" style="margin-top:20px">
              <label class="form-label">미용실 소개</label>
              <textarea v-model="profile.salonDescription" class="form-input" rows="2" placeholder="미용실을 소개해 주세요..."></textarea>
            </div>
            <div class="form-group" style="margin-top:12px">
              <label class="form-label">자기 소개</label>
              <textarea v-model="profile.bio" class="form-input" rows="4" placeholder="자신을 소개해 주세요..."></textarea>
            </div>
            <p v-if="basicMsg" :class="basicSuccess ? 'success-msg' : 'error-msg'">{{ basicMsg }}</p>
            <div class="save-row">
              <button class="btn btn-primary" :disabled="basicSaving" @click="saveBasic">
                <span v-if="basicSaving" class="spinner" style="width:16px;height:16px;border-width:2px;"></span>
                <span v-else>저장하기</span>
              </button>
            </div>
          </div>

          <!-- 영업시간 -->
          <div v-if="activeSection === 'hours'" class="card">
            <h2 class="manage-title">영업시간</h2>
            <div class="hours-editor">
              <div v-for="h in hoursForm" :key="h.dayIndex" class="hour-row">
                <span class="hour-day">{{ h.day }}</span>
                <label class="toggle-wrap">
                  <input type="checkbox" v-model="h.open" />
                  <span class="toggle-text">{{ h.open ? '영업' : '휴무' }}</span>
                </label>
                <input v-if="h.open" v-model="h.start" type="time" class="form-input time-input" />
                <span v-if="h.open" class="tilde">~</span>
                <input v-if="h.open" v-model="h.end" type="time" class="form-input time-input" />
              </div>
            </div>
            <p v-if="hoursMsg" :class="hoursSuccess ? 'success-msg' : 'error-msg'">{{ hoursMsg }}</p>
            <div class="save-row">
              <button class="btn btn-primary" :disabled="hoursSaving" @click="saveHours">
                <span v-if="hoursSaving" class="spinner" style="width:16px;height:16px;border-width:2px;"></span>
                <span v-else>저장하기</span>
              </button>
            </div>
          </div>

          <!-- 서비스 & 가격 -->
          <div v-if="activeSection === 'services'" class="card">
            <h2 class="manage-title">서비스 &amp; 가격</h2>
            <div class="service-editor">
              <div v-for="(svc, i) in services" :key="i" class="svc-row">
                <select v-model="svc.category" class="form-input svc-category">
                  <option value="">카테고리</option>
                  <option v-for="c in serviceCategories" :key="c" :value="c">{{ c }}</option>
                </select>
                <input v-model="svc.name" class="form-input" placeholder="서비스명 (예: 단발컷)" />
                <input v-model.number="svc.durationMinutes" type="number" class="form-input" placeholder="시간(분)" min="1" />
                <input v-model.number="svc.price" type="number" class="form-input" placeholder="가격(원)" min="0" />
                <button class="icon-btn danger" @click="removeService(i, svc)">✕</button>
              </div>
            </div>
            <button class="btn btn-ghost" style="margin-top:12px" @click="services.push({name:'',category:'커트',durationMinutes:60,price:0, isNew:true})">
              + 서비스 추가
            </button>
            <p v-if="svcMsg" :class="svcSuccess ? 'success-msg' : 'error-msg'">{{ svcMsg }}</p>
            <div class="save-row">
              <button class="btn btn-primary" :disabled="svcSaving" @click="saveServices">
                <span v-if="svcSaving" class="spinner" style="width:16px;height:16px;border-width:2px;"></span>
                <span v-else>저장하기</span>
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
  { id: 'basic', icon: '👤', label: '기본 정보' },
  { id: 'hours', icon: '🕐', label: '영업시간' },
  { id: 'services', icon: '💇', label: '서비스 & 가격' },
]

// ── 기본 정보 ────────────────────────────────────────────────
const profile = ref({ salonName: '', location: '', salonPhone: '', salonDescription: '', experience: 0, bio: '', profileImg: '' })
const basicMsg = ref('')
const basicSuccess = ref(false)
const basicSaving = ref(false)

// ── 영업시간 ────────────────────────────────────────────────
const hoursForm = ref(
  ['월', '화', '수', '목', '금', '토', '일'].map((day, i) => ({
    day, dayIndex: i, open: day !== '일', start: '10:00', end: '20:00',
  }))
)
const hoursMsg = ref('')
const hoursSuccess = ref(false)
const hoursSaving = ref(false)

// 서비스 카테고리 목록
const serviceCategories = ['커트', '펌', '염색', '케어', '두피관리', '기타']

// ── 서비스 ────────────────────────────────────────────────────
const services = ref([])
const svcMsg = ref('')
const svcSuccess = ref(false)
const svcSaving = ref(false)
const deletedServiceIds = ref([])

onMounted(async () => {
  try {
    const res = await stylistApi.getMyProfile()
    const data = res.data
    profile.value = {
      salonName:        data.salonName        || '',
      location:         data.location         || '',
      salonPhone:       data.salonPhone        || '',
      salonDescription: data.salonDescription  || '',
      experience:       data.experience        || 0,
      bio:              data.bio               || '',
      profileImg:       data.profileImg        || '',
    }
    services.value = (data.services || []).map(s => ({ ...s, isNew: false }))

    if (data.workingHours?.length) {
      data.workingHours.forEach(wh => {
        const h = hoursForm.value.find(f => f.dayIndex === wh.dayOfWeek)
        if (h) {
          h.open = !wh.isDayOff
          h.start = wh.openTime?.slice(0, 5) || '10:00'
          h.end = wh.closeTime?.slice(0, 5) || '20:00'
        }
      })
    }
  } catch (e) {
    console.error('미용사 정보 로드 실패:', e)
  }
})

async function saveBasic() {
  basicSaving.value = true
  basicMsg.value = ''
  try {
    await stylistApi.updateProfile({
      salonName:        profile.value.salonName,
      location:         profile.value.location,
      salonPhone:       profile.value.salonPhone       || null,
      salonDescription: profile.value.salonDescription || null,
      bio:              profile.value.bio,
      experience:       Number(profile.value.experience) || 0,
    })
    basicMsg.value = '저장되었습니다.'
    basicSuccess.value = true
  } catch (e) {
    basicMsg.value = e.response?.data?.message || '저장 실패'
    basicSuccess.value = false
  } finally {
    basicSaving.value = false
  }
}

async function saveHours() {
  hoursSaving.value = true
  hoursMsg.value = ''
  try {
    for (const h of hoursForm.value) {
      await stylistApi.updateHours({
        dayOfWeek: h.dayIndex,
        openTime: h.open ? `${h.start}:00` : null,
        closeTime: h.open ? `${h.end}:00` : null,
        isDayOff: !h.open,
      })
    }
    hoursMsg.value = '영업시간이 저장되었습니다.'
    hoursSuccess.value = true
  } catch (e) {
    hoursMsg.value = e.response?.data?.message || '저장 실패'
    hoursSuccess.value = false
  } finally {
    hoursSaving.value = false
  }
}

async function saveServices() {
  svcSaving.value = true
  svcMsg.value = ''
  try {
    // 새 서비스 추가
    for (const svc of services.value) {
      if (svc.isNew && svc.name) {
        const res = await stylistApi.addService({
          name: svc.name,
          category: svc.category || '기타',
          durationMinutes: svc.durationMinutes || 60,
          price: svc.price || 0,
          description: svc.description || '',
        })
        svc.id = res.data.id
        svc.isNew = false
      } else if (!svc.isNew && svc.id && svc.name) {
        await stylistApi.updateService(svc.id, {
          name: svc.name,
          category: svc.category || '기타',
          durationMinutes: svc.durationMinutes || 60,
          price: svc.price || 0,
          description: svc.description || '',
        })
      }
    }
    // 삭제 목록 처리
    for (const id of deletedServiceIds.value) {
      await stylistApi.deleteService(id)
    }
    deletedServiceIds.value = []
    svcMsg.value = '서비스가 저장되었습니다.'
    svcSuccess.value = true
  } catch (e) {
    svcMsg.value = e.response?.data?.message || '저장 실패'
    svcSuccess.value = false
  } finally {
    svcSaving.value = false
  }
}

function removeService(idx, svc) {
  if (svc.id && !svc.isNew) {
    deletedServiceIds.value.push(svc.id)
  }
  services.value.splice(idx, 1)
}

</script>

<style scoped>
.manage-layout { display: grid; grid-template-columns: 200px 1fr; gap: 24px; align-items: start; }
.manage-nav { display: flex; flex-direction: column; gap: 4px; position: sticky; top: 88px; }
.nav-item {
  display: flex; gap: 10px; align-items: center;
  padding: 12px 16px; border-radius: var(--radius-md);
  border: none; background: transparent;
  color: var(--color-text-secondary); font-size: 14px;
  cursor: pointer; transition: var(--transition); text-align: left;
}
.nav-item:hover { background: var(--color-bg-surface); color: var(--color-text-primary); }
.nav-item.active { background: rgba(201,169,110,0.1); color: var(--color-gold); font-weight: 600; }

.manage-title { font-size: 18px; font-weight: 700; margin-bottom: 20px; }
.manage-desc { font-size: 14px; color: var(--color-text-secondary); margin-bottom: 16px; }

.form-grid { display: grid; grid-template-columns: 120px 1fr; gap: 20px; align-items: start; }
.form-fields { display: flex; flex-direction: column; gap: 16px; }

.photo-upload { position: relative; width: 120px; height: 120px; border-radius: 50%; overflow: hidden; cursor: pointer; }
.current-photo { width: 100%; height: 100%; object-fit: cover; }
.photo-overlay {
  position: absolute; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; color: white; opacity: 0; transition: var(--transition);
}
.photo-upload:hover .photo-overlay { opacity: 1; }

.hours-editor { display: flex; flex-direction: column; gap: 10px; }
.hour-row { display: flex; align-items: center; gap: 10px; }
.hour-day { width: 28px; font-size: 14px; color: var(--color-text-secondary); }
.toggle-wrap { display: flex; align-items: center; gap: 6px; cursor: pointer; min-width: 52px; }
.toggle-text { font-size: 13px; color: var(--color-text-muted); }
.time-input { width: 110px; padding: 8px 10px; font-size: 13px; }
.tilde { color: var(--color-text-muted); }

.service-editor { display: flex; flex-direction: column; gap: 10px; }
.svc-row { display: grid; grid-template-columns: 120px 1fr 80px 1fr auto; gap: 8px; align-items: center; }
.svc-category { font-size: 13px; }
.icon-btn {
  width: 32px; height: 32px; border-radius: var(--radius-sm);
  border: 1px solid var(--color-border); background: transparent;
  cursor: pointer; font-size: 16px; display: flex; align-items: center; justify-content: center;
}
.icon-btn.danger { color: var(--color-danger); border-color: var(--color-danger); }


.save-row { display: flex; justify-content: flex-end; margin-top: 24px; padding-top: 20px; border-top: 1px solid var(--color-border); }
.success-msg { color: var(--color-success, #4ade80); font-size: 13px; margin-bottom: 8px; }
.error-msg { color: var(--color-danger); font-size: 13px; margin-bottom: 8px; }

@media (max-width: 768px) {
  .manage-layout { grid-template-columns: 1fr; }
  .manage-nav { flex-direction: row; overflow-x: auto; position: static; }
  .form-grid { grid-template-columns: 1fr; }
  .svc-row { grid-template-columns: 1fr 1fr; }
}
</style>
