import React, { useState } from 'react'
import { View, Text, TouchableOpacity, StyleSheet, Alert, ActivityIndicator } from 'react-native'
import { paymentApi } from '../api/payment'

export default function PaymentScreen({ route, navigation }) {
  const { reservation, service } = route.params
  const [loading, setLoading] = useState(false)

  async function handlePayment() {
    setLoading(true)
    try {
      await paymentApi.prepare({
        reservationId: reservation.id,
        amount: service.price,
        method: 'TOSS',
      })
      // 실제 환경: Toss 모바일 SDK 또는 WebView로 결제창 띄우기
      // 여기서는 mock confirm 처리
      const mockPaymentKey = `mock_${Date.now()}`
      await paymentApi.confirm({
        paymentKey: mockPaymentKey,
        orderId: `order_${reservation.id}`,
        amount: service.price,
      })
      Alert.alert('결제 완료', '예약이 완료되었습니다.', [
        { text: '확인', onPress: () => navigation.popToTop() },
      ])
    } catch (e) {
      Alert.alert('결제 실패', e.response?.data?.message ?? '결제에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <View style={styles.container}>
      <View style={styles.card}>
        <Text style={styles.title}>결제 정보</Text>
        <View style={styles.row}>
          <Text style={styles.rowLabel}>서비스</Text>
          <Text style={styles.rowValue}>{service.name}</Text>
        </View>
        <View style={styles.row}>
          <Text style={styles.rowLabel}>소요 시간</Text>
          <Text style={styles.rowValue}>{service.durationMinutes}분</Text>
        </View>
        <View style={styles.divider} />
        <View style={styles.row}>
          <Text style={styles.totalLabel}>총 결제 금액</Text>
          <Text style={styles.totalValue}>{service.price?.toLocaleString()}원</Text>
        </View>
      </View>

      <TouchableOpacity
        style={[styles.btn, loading && styles.btnDisabled]}
        onPress={handlePayment}
        disabled={loading}
      >
        {loading
          ? <ActivityIndicator color="#fff" />
          : <Text style={styles.btnText}>결제하기</Text>
        }
      </TouchableOpacity>

      <TouchableOpacity style={styles.cancelBtn} onPress={() => navigation.goBack()}>
        <Text style={styles.cancelText}>취소</Text>
      </TouchableOpacity>
    </View>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8f9fa', padding: 16 },
  card: {
    backgroundColor: '#fff', borderRadius: 12, padding: 20,
    marginBottom: 20, shadowColor: '#000', shadowOpacity: 0.06,
    shadowRadius: 8, elevation: 2,
  },
  title: { fontSize: 17, fontWeight: '700', color: '#1a1a2e', marginBottom: 16 },
  row: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 8 },
  rowLabel: { fontSize: 14, color: '#888' },
  rowValue: { fontSize: 14, color: '#1a1a2e', fontWeight: '500' },
  divider: { height: 1, backgroundColor: '#f0f0f0', marginVertical: 8 },
  totalLabel: { fontSize: 15, fontWeight: '700', color: '#1a1a2e' },
  totalValue: { fontSize: 18, fontWeight: '800', color: '#6c63ff' },
  btn: {
    backgroundColor: '#6c63ff', borderRadius: 12,
    paddingVertical: 16, alignItems: 'center',
  },
  btnDisabled: { opacity: 0.6 },
  btnText: { color: '#fff', fontSize: 16, fontWeight: '700' },
  cancelBtn: { alignItems: 'center', marginTop: 14 },
  cancelText: { color: '#888', fontSize: 14 },
})
