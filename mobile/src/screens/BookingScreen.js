import React, { useState, useEffect } from 'react'
import {
  View, Text, ScrollView, TouchableOpacity,
  StyleSheet, Alert, ActivityIndicator,
} from 'react-native'
import { reservationApi } from '../api/reservation'

const TIMES = ['09:00', '10:00', '11:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00']

function getNext7Days() {
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date()
    d.setDate(d.getDate() + i + 1)
    return d.toISOString().split('T')[0]
  })
}

export default function BookingScreen({ route, navigation }) {
  const { stylist } = route.params
  const [selectedDate, setSelectedDate] = useState(null)
  const [selectedTime, setSelectedTime] = useState(null)
  const [selectedService, setSelectedService] = useState(null)
  const [bookedTimes, setBookedTimes] = useState([])
  const [loading, setLoading] = useState(false)
  const days = getNext7Days()

  useEffect(() => {
    if (!selectedDate) return
    reservationApi.getBookedTimes(stylist.id, selectedDate)
      .then((res) => setBookedTimes(res.data ?? []))
      .catch(() => {})
  }, [selectedDate, stylist.id])

  async function handleBook() {
    if (!selectedDate || !selectedTime || !selectedService) {
      Alert.alert('선택 필요', '날짜, 시간, 서비스를 모두 선택해주세요.')
      return
    }
    setLoading(true)
    try {
      const res = await reservationApi.create({
        stylistId: stylist.id,
        serviceItemId: selectedService.id,
        reservedAt: `${selectedDate}T${selectedTime}:00`,
        totalPrice: selectedService.price,
      })
      navigation.replace('Payment', { reservation: res.data, service: selectedService })
    } catch (e) {
      Alert.alert('예약 실패', e.response?.data?.message ?? '예약에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.label}>날짜 선택</Text>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ marginBottom: 16 }}>
        {days.map((d) => (
          <TouchableOpacity
            key={d}
            style={[styles.chip, selectedDate === d && styles.chipActive]}
            onPress={() => { setSelectedDate(d); setSelectedTime(null) }}
          >
            <Text style={[styles.chipText, selectedDate === d && styles.chipTextActive]}>
              {d.slice(5)}
            </Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      <Text style={styles.label}>시간 선택</Text>
      <View style={styles.timeGrid}>
        {TIMES.map((t) => {
          const booked = bookedTimes.includes(t)
          return (
            <TouchableOpacity
              key={t}
              style={[styles.timeChip, selectedTime === t && styles.chipActive, booked && styles.timeBooked]}
              onPress={() => !booked && setSelectedTime(t)}
              disabled={booked}
            >
              <Text style={[styles.chipText, selectedTime === t && styles.chipTextActive, booked && styles.timeBookedText]}>
                {t}
              </Text>
            </TouchableOpacity>
          )
        })}
      </View>

      <Text style={styles.label}>서비스 선택</Text>
      {stylist.services?.map((s) => (
        <TouchableOpacity
          key={s.id}
          style={[styles.serviceRow, selectedService?.id === s.id && styles.serviceRowActive]}
          onPress={() => setSelectedService(s)}
        >
          <View style={{ flex: 1 }}>
            <Text style={styles.serviceName}>{s.name}</Text>
            <Text style={styles.serviceMeta}>{s.durationMinutes}분</Text>
          </View>
          <Text style={styles.servicePrice}>{s.price?.toLocaleString()}원</Text>
        </TouchableOpacity>
      ))}

      <TouchableOpacity
        style={[styles.btn, loading && styles.btnDisabled]}
        onPress={handleBook}
        disabled={loading}
      >
        {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.btnText}>예약 확정 및 결제하기</Text>}
      </TouchableOpacity>
    </ScrollView>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8f9fa', padding: 16 },
  label: { fontSize: 15, fontWeight: '700', color: '#1a1a2e', marginBottom: 10, marginTop: 6 },
  chip: {
    paddingHorizontal: 16, paddingVertical: 8, borderRadius: 20,
    borderWidth: 1.5, borderColor: '#ddd', marginRight: 8, backgroundColor: '#fff',
  },
  chipActive: { backgroundColor: '#6c63ff', borderColor: '#6c63ff' },
  chipText: { fontSize: 13, color: '#555' },
  chipTextActive: { color: '#fff', fontWeight: '600' },
  timeGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 16 },
  timeChip: {
    paddingHorizontal: 14, paddingVertical: 8, borderRadius: 8,
    borderWidth: 1.5, borderColor: '#ddd', backgroundColor: '#fff',
  },
  timeBooked: { backgroundColor: '#f0f0f0', borderColor: '#e0e0e0' },
  timeBookedText: { color: '#bbb' },
  serviceRow: {
    flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff',
    borderRadius: 10, padding: 14, marginBottom: 8,
    borderWidth: 1.5, borderColor: '#eee',
  },
  serviceRowActive: { borderColor: '#6c63ff', backgroundColor: '#f5f3ff' },
  serviceName: { fontSize: 14, fontWeight: '600', color: '#1a1a2e' },
  serviceMeta: { fontSize: 12, color: '#888', marginTop: 2 },
  servicePrice: { fontSize: 15, fontWeight: '700', color: '#6c63ff' },
  btn: {
    backgroundColor: '#6c63ff', borderRadius: 12,
    paddingVertical: 16, alignItems: 'center', margin: 8, marginTop: 20,
  },
  btnDisabled: { opacity: 0.6 },
  btnText: { color: '#fff', fontSize: 16, fontWeight: '700' },
})
