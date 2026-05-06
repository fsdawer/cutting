import React, { useState, useEffect, useCallback } from 'react'
import {
  View, Text, FlatList, TextInput, TouchableOpacity,
  StyleSheet, ActivityIndicator, Alert,
} from 'react-native'
import * as Location from 'expo-location'
import { WebView } from 'react-native-webview'
import StylistCard from '../components/StylistCard'
import { stylistApi } from '../api/stylist'

const KAKAO_JS_KEY = 'c9d4907397682ac98da88232486e265c'

export default function HomeScreen({ navigation }) {
  const [keyword, setKeyword] = useState('')
  const [stylists, setStylists] = useState([])
  const [loading, setLoading] = useState(false)
  const [nearbyMode, setNearbyMode] = useState(false)
  const [mapHtml, setMapHtml] = useState('')

  useEffect(() => { fetchStylists() }, [])

  async function fetchStylists(kw = '') {
    setLoading(true)
    try {
      const params = kw ? { keyword: kw } : {}
      const res = await stylistApi.getStylists(params)
      setStylists(res.data)
      setNearbyMode(false)
      setMapHtml('')
    } catch {
      Alert.alert('오류', '목록을 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  async function searchNearby() {
    const { status } = await Location.requestForegroundPermissionsAsync()
    if (status !== 'granted') {
      Alert.alert('권한 필요', '위치 권한을 허용해주세요.')
      return
    }
    setLoading(true)
    try {
      const loc = await Location.getCurrentPositionAsync({})
      const { latitude: lat, longitude: lng } = loc.coords
      const res = await stylistApi.getNearbyStylists(lat, lng, 3000)
      setStylists(res.data)
      setNearbyMode(true)
      setMapHtml(buildMapHtml(lat, lng, res.data))
    } catch {
      Alert.alert('오류', '주변 미용사를 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  function buildMapHtml(lat, lng, list) {
    const markers = list
      .filter((s) => s.latitude && s.longitude)
      .map(
        (s) => `
        var marker = new kakao.maps.Marker({
          map: map,
          position: new kakao.maps.LatLng(${s.latitude}, ${s.longitude}),
          title: "${s.name}"
        });
        var iw = new kakao.maps.InfoWindow({
          content: '<div style="padding:6px 10px;font-size:12px"><b>${s.name}</b><br/>${s.salonName ?? ''}</div>'
        });
        kakao.maps.event.addListener(marker, 'click', function() { iw.open(map, marker); });
      `
      )
      .join('\n')

    return `
      <!DOCTYPE html><html><head>
      <meta name="viewport" content="width=device-width,initial-scale=1">
      <script src="//dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}"></script>
      </head><body style="margin:0">
      <div id="map" style="width:100%;height:100vh"></div>
      <script>
        var map = new kakao.maps.Map(document.getElementById('map'), {
          center: new kakao.maps.LatLng(${lat}, ${lng}),
          level: 5
        });
        new kakao.maps.Marker({ map: map, position: new kakao.maps.LatLng(${lat}, ${lng}),
          image: new kakao.maps.MarkerImage(
            'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/markerStar.png',
            new kakao.maps.Size(24,35))
        });
        ${markers}
      </script></body></html>`
  }

  const renderItem = useCallback(
    ({ item }) => (
      <StylistCard
        stylist={item}
        onPress={() => navigation.navigate('StylistDetail', { stylistId: item.id })}
      />
    ),
    [navigation]
  )

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>내 근처 미용사 예약</Text>
        <View style={styles.searchRow}>
          <TextInput
            style={styles.input}
            placeholder="미용사 이름, 미용실 검색"
            value={keyword}
            onChangeText={setKeyword}
            onSubmitEditing={() => fetchStylists(keyword)}
            returnKeyType="search"
          />
          <TouchableOpacity style={styles.btnPrimary} onPress={() => fetchStylists(keyword)}>
            <Text style={styles.btnText}>검색</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.btnOutline, nearbyMode && styles.btnOutlineActive]}
            onPress={searchNearby}
          >
            <Text style={[styles.btnOutlineText, nearbyMode && styles.btnOutlineTextActive]}>
              📍 내 주변
            </Text>
          </TouchableOpacity>
        </View>
      </View>

      {nearbyMode && mapHtml ? (
        <WebView source={{ html: mapHtml }} style={styles.map} />
      ) : null}

      {loading ? (
        <ActivityIndicator style={{ marginTop: 40 }} size="large" color="#6c63ff" />
      ) : (
        <FlatList
          data={stylists}
          keyExtractor={(item) => String(item.id)}
          renderItem={renderItem}
          contentContainerStyle={{ paddingVertical: 8 }}
          ListEmptyComponent={
            <Text style={styles.empty}>검색 결과가 없습니다</Text>
          }
        />
      )}
    </View>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8f9fa' },
  header: { backgroundColor: '#fff', padding: 16, borderBottomWidth: 1, borderBottomColor: '#eee' },
  title: { fontSize: 20, fontWeight: '700', color: '#1a1a2e', marginBottom: 12 },
  searchRow: { flexDirection: 'row', gap: 8, alignItems: 'center' },
  input: {
    flex: 1, borderWidth: 1.5, borderColor: '#ddd', borderRadius: 8,
    paddingHorizontal: 12, paddingVertical: 9, fontSize: 14,
  },
  btnPrimary: { backgroundColor: '#6c63ff', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 10 },
  btnText: { color: '#fff', fontSize: 14, fontWeight: '600' },
  btnOutline: { borderWidth: 1.5, borderColor: '#6c63ff', borderRadius: 8, paddingHorizontal: 12, paddingVertical: 10 },
  btnOutlineActive: { backgroundColor: '#6c63ff' },
  btnOutlineText: { color: '#6c63ff', fontSize: 13, fontWeight: '500' },
  btnOutlineTextActive: { color: '#fff' },
  map: { height: 260 },
  empty: { textAlign: 'center', color: '#aaa', marginTop: 60, fontSize: 15 },
})
