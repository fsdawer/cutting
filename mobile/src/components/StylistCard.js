import React from 'react'
import { View, Text, TouchableOpacity, StyleSheet, Image } from 'react-native'

export default function StylistCard({ stylist, onPress }) {
  return (
    <TouchableOpacity style={styles.card} onPress={onPress} activeOpacity={0.85}>
      <Image
        source={
          stylist.profileImg
            ? { uri: stylist.profileImg }
            : require('../../assets/default-avatar.png')
        }
        style={styles.avatar}
      />
      <View style={styles.info}>
        <Text style={styles.name}>{stylist.name}</Text>
        <Text style={styles.salon}>{stylist.salonName ?? '미용실 미등록'}</Text>
        <Text style={styles.location} numberOfLines={1}>{stylist.location ?? ''}</Text>
        <View style={styles.meta}>
          <Text style={styles.rating}>⭐ {stylist.rating?.toFixed(1) ?? '0.0'}</Text>
          <Text style={styles.review}>리뷰 {stylist.reviewCount ?? 0}개</Text>
          <Text style={styles.exp}>경력 {stylist.experience ?? 0}년</Text>
        </View>
      </View>
    </TouchableOpacity>
  )
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 14,
    marginHorizontal: 16,
    marginVertical: 6,
    shadowColor: '#000',
    shadowOpacity: 0.06,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 2 },
    elevation: 2,
  },
  avatar: { width: 64, height: 64, borderRadius: 32, backgroundColor: '#eee' },
  info: { flex: 1, marginLeft: 14, justifyContent: 'center' },
  name: { fontSize: 15, fontWeight: '700', color: '#1a1a2e', marginBottom: 2 },
  salon: { fontSize: 13, color: '#6c63ff', marginBottom: 2 },
  location: { fontSize: 12, color: '#888', marginBottom: 6 },
  meta: { flexDirection: 'row', gap: 10 },
  rating: { fontSize: 12, color: '#444' },
  review: { fontSize: 12, color: '#888' },
  exp: { fontSize: 12, color: '#888' },
})
