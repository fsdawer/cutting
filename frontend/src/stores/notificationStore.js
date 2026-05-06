import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const connected = ref(false)
  let stompClient = null

  const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

  function addNotification(msg) {
    notifications.value.unshift({
      id: `${Date.now()}-${Math.random()}`,
      type: msg.type,
      reservationId: msg.reservationId,
      message: msg.message,
      detail: msg.type === 'RESERVATION_CREATED'
        ? `${msg.clientName} · ${formatDateTime(msg.reservedAt)}`
        : msg.type === 'WAITING_AVAILABLE'
        ? `지금 바로 예약하세요 · ${formatDateTime(msg.reservedAt)}`
        : msg.reservedAt,
      read: false,
      createdAt: new Date().toISOString(),
    })
    // 최대 50개 유지
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
    if (stompClient?.active) return

    stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        connected.value = true
        stompClient.subscribe(`/topic/notification/${userId}`, (frame) => {
          try {
            const msg = JSON.parse(frame.body)
            addNotification(msg)
          } catch (e) {
            console.error('알림 파싱 오류', e)
          }
        })
      },
      onDisconnect: () => { connected.value = false },
      onStompError: () => { connected.value = false },
    })
    stompClient.activate()
  }

  function disconnect() {
    stompClient?.deactivate()
    connected.value = false
  }

  return { notifications, unreadCount, connected, connect, disconnect, markAllRead, markRead, clearAll }
})
