<template>
  <main class="page">
    <div class="container">
      <div class="page-header">
        <button class="back-btn" @click="$router.back()">‹</button>
        <h1 class="page-title">리뷰 관리</h1>
      </div>

      <div v-if="loading" class="state-center"><div class="spinner"></div></div>
      <div v-else-if="reviews.length === 0" class="state-center">
        <p class="state-text">작성한 리뷰가 없습니다.</p>
      </div>
      <div v-else class="review-list">
        <div v-for="r in reviews" :key="r.id" class="review-card card">
          <div class="review-header">
            <div>
              <p class="review-stylist">{{ r.stylistName }}</p>
              <p class="review-salon">{{ r.salonName || '' }} · {{ r.serviceName }}</p>
            </div>
            <div class="review-actions">
              <button class="btn btn-ghost btn-sm" @click="openEdit(r)">수정</button>
              <button class="btn btn-danger-ghost btn-sm" @click="deleteReview(r.id)">삭제</button>
            </div>
          </div>
          <div class="review-stars">
            <span v-for="i in 5" :key="i" :class="i <= r.rating ? 'star-filled' : 'star-empty'">★</span>
            <span class="review-date">{{ formatDate(r.createdAt) }}</span>
          </div>
          <p class="review-content">{{ r.content }}</p>
        </div>
      </div>
    </div>

    <!-- 수정 모달 -->
    <Teleport to="body">
      <div v-if="editModal.open" class="modal-backdrop" @click.self="editModal.open = false">
        <div class="modal-box">
          <div class="modal-header">
            <h3>리뷰 수정</h3>
            <button class="modal-close" @click="editModal.open = false">×</button>
          </div>
          <div class="modal-body">
            <div class="star-picker">
              <button v-for="i in 5" :key="i" class="star-pick-btn" :class="{ filled: i <= editModal.rating }" @click="editModal.rating = i">★</button>
            </div>
            <textarea v-model="editModal.content" class="form-input" rows="4" placeholder="리뷰 내용"></textarea>
            <p v-if="editModal.error" class="msg-error">{{ editModal.error }}</p>
          </div>
          <div class="modal-footer">
            <button class="btn btn-ghost" @click="editModal.open = false">취소</button>
            <button class="btn btn-primary" :disabled="editModal.saving" @click="submitEdit">
              <span v-if="editModal.saving" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
              <span v-else>저장</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { reviewApi } from '@/api/review'

const reviews = ref([])
const loading = ref(false)
const editModal = ref({ open: false, reviewId: null, rating: 5, content: '', saving: false, error: '' })

function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')}`
}

function openEdit(r) {
  editModal.value = { open: true, reviewId: r.id, rating: Number(r.rating), content: r.content, saving: false, error: '' }
}

async function submitEdit() {
  editModal.value.saving = true; editModal.value.error = ''
  try {
    await reviewApi.update(editModal.value.reviewId, { rating: editModal.value.rating, content: editModal.value.content })
    editModal.value.open = false
    await load()
  } catch (e) { editModal.value.error = e.response?.data?.message || '수정 실패' }
  finally { editModal.value.saving = false }
}

async function deleteReview(id) {
  if (!confirm('리뷰를 삭제하시겠습니까?')) return
  try { await reviewApi.remove(id); await load() }
  catch (e) { alert(e.response?.data?.message || '삭제 실패') }
}

async function load() {
  loading.value = true
  try { reviews.value = (await reviewApi.getMyReviews()).data || [] }
  catch { reviews.value = [] }
  finally { loading.value = false }
}

onMounted(load)
</script>

<style scoped>
.container { max-width: 480px; padding: 16px; }
.page-header { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.back-btn { font-size: 24px; background: none; border: none; cursor: pointer; color: var(--text); padding: 0 4px; line-height: 1; }
.page-title { font-size: 20px; font-weight: 800; }

.review-list { display: flex; flex-direction: column; gap: 10px; }
.review-card { padding: 16px; }
.review-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 8px; }
.review-stylist { font-size: 15px; font-weight: 700; }
.review-salon { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.review-actions { display: flex; gap: 6px; flex-shrink: 0; }
.review-stars { display: flex; align-items: center; gap: 2px; margin-bottom: 8px; }
.star-filled { color: var(--gold, #f59e0b); font-size: 16px; }
.star-empty { color: var(--border-strong); font-size: 16px; }
.review-date { font-size: 11px; color: var(--text-muted); margin-left: 8px; }
.review-content { font-size: 13px; color: var(--text); line-height: 1.6; }

.state-center { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 60px 0; }
.state-text { color: var(--text-muted); font-size: 15px; }

.modal-backdrop { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 500; padding: 16px; }
.modal-box { background: var(--bg-card); border-radius: var(--radius-xl); width: 100%; max-width: 400px; box-shadow: var(--shadow-lg); overflow: hidden; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 20px 0; }
.modal-header h3 { font-size: 17px; font-weight: 700; }
.modal-close { background: none; border: none; font-size: 22px; color: var(--text-muted); cursor: pointer; }
.modal-body { padding: 16px 20px; display: flex; flex-direction: column; gap: 12px; }
.modal-footer { display: flex; gap: 8px; justify-content: flex-end; padding: 0 20px 20px; }
.star-picker { display: flex; gap: 4px; }
.star-pick-btn { font-size: 26px; background: none; border: none; color: var(--border-strong); cursor: pointer; }
.star-pick-btn.filled { color: var(--gold, #f59e0b); }
.msg-error { color: var(--danger); font-size: 13px; }
</style>
