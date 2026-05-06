import React, { useState, useEffect } from 'react'
import {
  View, Text, ScrollView, TouchableOpacity,
  StyleSheet, ActivityIndicator, Alert, Image,
} from 'react-native'
import { stylistApi } from '../api/stylist'

export default function StylistDetailScreen({ route, navigation }) {
  const { stylistId } = route.params
  const [stylist, setStylist] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    stylistApi.getStylist(stylistId)
      .then((res) => setStylist(res.data))
      .catch(() => Alert.alert('오류', '정보를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [stylistId])

  if (loading) return <ActivityIndicator style={{ flex: 1 }} size="large" color="#6c63ff" />

  return (
    <ScrollView style={styles.container}>
      <View style={styles.profileSection}>
        <Image
          source={stylist.profileImg ? { uri: stylist.profileImg } : require('../../assets/default-avatar.png')}
          style={styles.avatar}
        />
        <Text style={styles.name}>{stylist.name}</Text>
        <Text style={styles.salon}>{stylist.salonName ?? '미용실 미등록'}</Text>
        <Text style={styles.location}>{stylist.location ?? ''}</Text>
        <View style={styles.metaRow}>
          <Text style={styles.meta}>⭐ {stylist.rating?.toFixed(1) ?? '0.0'}</Text>
          <Text style={styles.meta}>리뷰 {stylist.reviewCount ?? 0}개</Text>
          <Text style={styles.meta}>경력 {stylist.experience ?? 0}년</Text>
        </View>
        {stylist.bio ? <Text style={styles.bio}>{stylist.bio}</Text> : null}
      </View>

      {stylist.services?.length > 0 && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>제공 서비스</Text>
          {stylist.services.map((s) => (
            <View key={s.id} style={styles.serviceRow}>
              <View style={{ flex: 1 }}>
                <Text style={styles.serviceName}>{s.name}</Text>
                <Text style={styles.serviceMeta}>{s.category} · {s.durationMinutes}분</Text>
              </View>
              <Text style={styles.servicePrice}>{s.price?.toLocaleString()}원</Text>
            </View>
          ))}
        </View>
      )}

      <TouchableOpacity
        style={styles.bookingBtn}
        onPress={() => navigation.navigate('Booking', { stylist })}
      >
        <Text style={styles.bookingBtnText}>예약하기</Text>
      </TouchableOpacity>
    </ScrollView>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8f9fa' },
  profileSection: { backgroundColor: '#fff', alignItems: 'center', padding: 24, marginBottom: 8 },
  avatar: { width: 90, height: 90, borderRadius: 45, backgroundColor: '#eee', marginBottom: 12 },
  name: { fontSize: 20, fontWeight: '700', color: '#1a1a2e' },
  salon: { fontSize: 14, color: '#6c63ff', marginTop: 4 },
  location: { fontSize: 13, color: '#888', marginTop: 2 },
  metaRow: { flexDirection: 'row', gap: 14, marginTop: 10 },
  meta: { fontSize: 13, color: '#555' },
  bio: { fontSize: 14, color: '#444', marginTop: 14, textAlign: 'center', lineHeight: 20 },
  section: { backgroundColor: '#fff', padding: 16, marginBottom: 8 },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: '#1a1a2e', marginBottom: 12 },
  serviceRow: {
    flexDirection: 'row', alignItems: 'center',
    paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: '#f0f0f0',
  },
  serviceName: { fontSize: 14, fontWeight: '600', color: '#1a1a2e' },
  serviceMeta: { fontSize: 12, color: '#888', marginTop: 2 },
  servicePrice: { fontSize: 15, fontWeight: '700', color: '#6c63ff' },
  bookingBtn: {
    backgroundColor: '#6c63ff', margin: 16, borderRadius: 12,
    paddingVertical: 16, alignItems: 'center',
  },
  bookingBtnText: { color: '#fff', fontSize: 16, fontWeight: '700' },
})
