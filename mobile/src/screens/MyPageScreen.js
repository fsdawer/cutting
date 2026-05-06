import React, { useEffect, useState } from 'react'
import {
  View, Text, ScrollView, TouchableOpacity,
  StyleSheet, Alert, ActivityIndicator,
} from 'react-native'
import useAuthStore from '../store/authStore'
import { authApi } from '../api/auth'
import { reservationApi } from '../api/reservation'

const STATUS_LABEL = {
  PENDING: '대기 중', CONFIRMED: '확정', COMPLETED: '완료', CANCELLED: '취소',
}
const STATUS_COLOR = {
  PENDING: '#f59e0b', CONFIRMED: '#6c63ff', COMPLETED: '#10b981', CANCELLED: '#ef4444',
}

export default function MyPageScreen({ navigation }) {
  const { user, logout } = useAuthStore()
  const [reservations, setReservations] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    reservationApi.getMyReservations()
      .then((res) => setReservations(res.data ?? []))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  async function handleLogout() {
    try { await authApi.logout() } catch { /* 무시 */ }
    await logout()
  }

  async function handleCancel(id) {
    Alert.alert('예약 취소', '예약을 취소하시겠습니까?', [
      { text: '아니오' },
      {
        text: '취소하기', style: 'destructive',
        onPress: async () => {
          try {
            await reservationApi.cancel(id)
            setReservations((prev) => prev.map((r) => r.id === id ? { ...r, status: 'CANCELLED' } : r))
          } catch (e) {
            Alert.alert('오류', e.response?.data?.message ?? '취소에 실패했습니다.')
          }
        },
      },
    ])
  }

  return (
    <ScrollView style={styles.container}>
      <View style={styles.profile}>
        <Text style={styles.avatar}>👤</Text>
        <Text style={styles.name}>{user?.name ?? '사용자'}</Text>
        <Text style={styles.email}>{user?.email ?? ''}</Text>
        {user?.role === 'STYLIST' && (
          <TouchableOpacity style={styles.manageBtn} onPress={() => navigation.navigate('StylistManage')}>
            <Text style={styles.manageBtnText}>프로필 관리</Text>
          </TouchableOpacity>
        )}
      </View>

      <Text style={styles.sectionTitle}>내 예약 내역</Text>
      {loading ? (
        <ActivityIndicator color="#6c63ff" style={{ marginTop: 20 }} />
      ) : reservations.length === 0 ? (
        <Text style={styles.empty}>예약 내역이 없습니다</Text>
      ) : (
        reservations.map((r) => (
          <View key={r.id} style={styles.resCard}>
            <View style={styles.resHeader}>
              <Text style={styles.resName}>{r.stylistName}</Text>
              <Text style={[styles.resBadge, { color: STATUS_COLOR[r.status] }]}>
                {STATUS_LABEL[r.status] ?? r.status}
              </Text>
            </View>
            <Text style={styles.resMeta}>{r.serviceName} · {r.reservedAt?.slice(0, 16).replace('T', ' ')}</Text>
            <Text style={styles.resPrice}>{r.totalPrice?.toLocaleString()}원</Text>
            {r.status === 'PENDING' || r.status === 'CONFIRMED' ? (
              <TouchableOpacity style={styles.cancelLink} onPress={() => handleCancel(r.id)}>
                <Text style={styles.cancelLinkText}>예약 취소</Text>
              </TouchableOpacity>
            ) : null}
          </View>
        ))
      )}

      <TouchableOpacity style={styles.logoutBtn} onPress={handleLogout}>
        <Text style={styles.logoutText}>로그아웃</Text>
      </TouchableOpacity>
    </ScrollView>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8f9fa' },
  profile: {
    backgroundColor: '#fff', alignItems: 'center', padding: 28,
    borderBottomWidth: 1, borderBottomColor: '#eee', marginBottom: 8,
  },
  avatar: { fontSize: 52, marginBottom: 10 },
  name: { fontSize: 18, fontWeight: '700', color: '#1a1a2e' },
  email: { fontSize: 13, color: '#888', marginTop: 4 },
  manageBtn: {
    marginTop: 14, borderWidth: 1.5, borderColor: '#6c63ff',
    borderRadius: 8, paddingHorizontal: 20, paddingVertical: 8,
  },
  manageBtnText: { color: '#6c63ff', fontSize: 14, fontWeight: '600' },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: '#1a1a2e', margin: 16, marginBottom: 8 },
  empty: { textAlign: 'center', color: '#aaa', marginTop: 30, fontSize: 14 },
  resCard: {
    backgroundColor: '#fff', margin: 16, marginTop: 0, marginBottom: 8,
    borderRadius: 12, padding: 16, shadowColor: '#000', shadowOpacity: 0.05,
    shadowRadius: 6, elevation: 1,
  },
  resHeader: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 6 },
  resName: { fontSize: 15, fontWeight: '700', color: '#1a1a2e' },
  resBadge: { fontSize: 13, fontWeight: '600' },
  resMeta: { fontSize: 13, color: '#888', marginBottom: 4 },
  resPrice: { fontSize: 14, fontWeight: '700', color: '#6c63ff' },
  cancelLink: { marginTop: 8 },
  cancelLinkText: { color: '#ef4444', fontSize: 13 },
  logoutBtn: {
    margin: 16, borderWidth: 1, borderColor: '#ddd',
    borderRadius: 10, paddingVertical: 14, alignItems: 'center',
  },
  logoutText: { color: '#888', fontSize: 15 },
})
