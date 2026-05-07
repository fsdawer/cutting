import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const connected = ref(false)
  let eventSource = null

  const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

  function addNotification(msg) {
    // 같은 type + reservationId(또는 reservedAt) 조합은 중복 수신 방지
    const dedupKey = `${msg.type}_${msg.reservationId ?? msg.reservedAt ?? ''}`
    if (dedupKey && notifications.value.some(n => n.dedupKey === dedupKey)) return

    notifications.value.unshift({
      id: `${Date.now()}-${Math.random()}`,
      dedupKey,
      type: msg.type,
      reservationId: msg.reservationId,
      message: msg.message,
      detail: msg.type === 'RESERVATION_CREATED'
        ? `${msg.clientName} · ${formatDateTime(msg.reservedAt)}`
        : msg.type === 'WAITING_AVAILABLE'
        ? `지금 바로 예약하세요 · ${formatDateTime(msg.reservedAt)}`
        : msg.type === 'RESERVATION_REMINDER_1D'
        ? `내일 예약 · ${formatDateTime(msg.reservedAt)}`
        : msg.type === 'RESERVATION_REMINDER_1H'
        ? `1시간 후 예약 · ${formatDateTime(msg.reservedAt)}`
        : msg.reservedAt,
      read: false,
      createdAt: new Date().toISOString(),
    })
    if (notifications.value.length > 50) notifications.value.pop()
  }

  function formatDateTime(str) {
    if (!str) return ''
    try {
      const d = new Date(str)
      return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
    } catch { return str }
  }

  function markAllRead() {
    notifications.value.forEach(n => { n.read = true })
  }

  function markRead(id) {
    const n = notifications.value.find(n => n.id === id)
    if (n) n.read = true
  }

  function clearAll() {
    notifications.value = []
  }

  function connect(userId, token) {
    if (eventSource) return

    // EventSource는 커스텀 헤더 미지원 → token을 query param으로 전달
    const url = `http://localhost:8080/api/notifications/stream?token=${encodeURIComponent(token)}`
    eventSource = new EventSource(url)

    eventSource.addEventListener('notification', (e) => {
      try {
        addNotification(JSON.parse(e.data))
      } catch (err) {
        console.error('알림 파싱 오류', err)
      }
    })

    eventSource.onopen  = () => { connected.value = true  }
    eventSource.onerror = () => { connected.value = false }
  }

  function disconnect() {
    eventSource?.close()
    eventSource = null
    connected.value = false
  }

  return { notifications, unreadCount, connected, connect, disconnect, markAllRead, markRead, clearAll }
})
