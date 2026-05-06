import React, { useState, useEffect } from 'react'
import {
  View, Text, TextInput, ScrollView, TouchableOpacity,
  StyleSheet, Alert, ActivityIndicator,
} from 'react-native'
import { stylistApi } from '../api/stylist'

export default function StylistManageScreen() {
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState({ bio: '', experience: '', salonName: '', location: '', salonPhone: '' })
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    stylistApi.getMyProfile()
      .then((res) => {
        const p = res.data
        setProfile(p)
        setForm({
          bio: p.bio ?? '',
          experience: String(p.experience ?? ''),
          salonName: p.salonName ?? '',
          location: p.location ?? '',
          salonPhone: p.salonPhone ?? '',
        })
      })
      .catch(() => Alert.alert('오류', '프로필을 불러오지 못했습니다.'))
  }, [])

  function update(key, val) { setForm((f) => ({ ...f, [key]: val })) }

  async function handleSave() {
    setSaving(true)
    try {
      await stylistApi.updateProfile({
        ...form,
        experience: form.experience ? parseInt(form.experience) : undefined,
      })
      Alert.alert('저장 완료', '프로필이 업데이트되었습니다.')
    } catch (e) {
      Alert.alert('저장 실패', e.response?.data?.message ?? '저장에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  if (!profile) return <ActivityIndicator style={{ flex: 1 }} size="large" color="#6c63ff" />

  return (
    <ScrollView style={styles.container} keyboardShouldPersistTaps="handled">
      <Text style={styles.section}>기본 정보</Text>
      {[
        { key: 'bio', label: '자기소개', multiline: true, numberOfLines: 4 },
        { key: 'experience', label: '경력 (년)', keyboardType: 'number-pad' },
      ].map(({ key, label, ...props }) => (
        <View key={key} style={styles.field}>
          <Text style={styles.label}>{label}</Text>
          <TextInput
            style={[styles.input, props.multiline && styles.textarea]}
            value={form[key]}
            onChangeText={(v) => update(key, v)}
            {...props}
          />
        </View>
      ))}

      <Text style={styles.section}>미용실 정보</Text>
      {[
        { key: 'salonName', label: '미용실 이름' },
        { key: 'location', label: '주소 (지도에 자동 반영)' },
        { key: 'salonPhone', label: '미용실 전화번호', keyboardType: 'phone-pad' },
      ].map(({ key, label, ...props }) => (
        <View key={key} style={styles.field}>
          <Text style={styles.label}>{label}</Text>
          <TextInput
            style={styles.input}
            value={form[key]}
            onChangeText={(v) => update(key, v)}
            {...props}
          />
        </View>
      ))}

      <TouchableOpacity
        style={[styles.btn, saving && styles.btnDisabled]}
        onPress={handleSave}
        disabled={saving}
      >
        {saving ? <ActivityIndicator color="#fff" /> : <Text style={styles.btnText}>저장하기</Text>}
      </TouchableOpacity>
    </ScrollView>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8f9fa', padding: 16 },
  section: { fontSize: 15, fontWeight: '700', color: '#6c63ff', marginTop: 16, marginBottom: 8 },
  field: { marginBottom: 14 },
  label: { fontSize: 13, color: '#555', marginBottom: 6 },
  input: {
    backgroundColor: '#fff', borderWidth: 1.5, borderColor: '#ddd',
    borderRadius: 10, paddingHorizontal: 14, paddingVertical: 11, fontSize: 14,
  },
  textarea: { height: 90, textAlignVertical: 'top' },
  btn: {
    backgroundColor: '#6c63ff', borderRadius: 12,
    paddingVertical: 15, alignItems: 'center', marginTop: 20, marginBottom: 40,
  },
  btnDisabled: { opacity: 0.6 },
  btnText: { color: '#fff', fontSize: 16, fontWeight: '700' },
})
