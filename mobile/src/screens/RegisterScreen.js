import React, { useState } from 'react'
import {
  View, Text, TextInput, TouchableOpacity, ScrollView,
  StyleSheet, Alert, KeyboardAvoidingView, Platform,
} from 'react-native'
import { authApi } from '../api/auth'

export default function RegisterScreen({ navigation }) {
  const [form, setForm] = useState({ username: '', email: '', password: '', name: '', phone: '' })
  const [loading, setLoading] = useState(false)

  function update(key, val) { setForm((f) => ({ ...f, [key]: val })) }

  async function handleRegister() {
    const required = ['username', 'email', 'password', 'name', 'phone']
    if (required.some((k) => !form[k])) {
      Alert.alert('입력 오류', '모든 항목을 입력해주세요.')
      return
    }
    setLoading(true)
    try {
      await authApi.register({ ...form, role: 'USER' })
      Alert.alert('가입 완료', '로그인 해주세요.', [
        { text: '확인', onPress: () => navigation.goBack() },
      ])
    } catch (e) {
      Alert.alert('가입 실패', e.response?.data?.message ?? '회원가입에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <KeyboardAvoidingView
      style={{ flex: 1 }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>회원가입</Text>

        {[
          { key: 'username', placeholder: '아이디', autoCapitalize: 'none' },
          { key: 'email', placeholder: '이메일', keyboardType: 'email-address', autoCapitalize: 'none' },
          { key: 'password', placeholder: '비밀번호', secureTextEntry: true },
          { key: 'name', placeholder: '이름' },
          { key: 'phone', placeholder: '전화번호 (010-0000-0000)', keyboardType: 'phone-pad' },
        ].map(({ key, ...props }) => (
          <TextInput
            key={key}
            style={styles.input}
            value={form[key]}
            onChangeText={(v) => update(key, v)}
            {...props}
          />
        ))}

        <TouchableOpacity
          style={[styles.btn, loading && styles.btnDisabled]}
          onPress={handleRegister}
          disabled={loading}
        >
          <Text style={styles.btnText}>{loading ? '처리 중...' : '가입하기'}</Text>
        </TouchableOpacity>
      </ScrollView>
    </KeyboardAvoidingView>
  )
}

const styles = StyleSheet.create({
  container: { padding: 24, gap: 14 },
  title: { fontSize: 22, fontWeight: '700', color: '#1a1a2e', marginBottom: 8 },
  input: {
    borderWidth: 1.5, borderColor: '#ddd', borderRadius: 10,
    paddingHorizontal: 16, paddingVertical: 13, fontSize: 15,
  },
  btn: {
    backgroundColor: '#6c63ff', borderRadius: 10,
    paddingVertical: 14, alignItems: 'center', marginTop: 6,
  },
  btnDisabled: { opacity: 0.6 },
  btnText: { color: '#fff', fontSize: 16, fontWeight: '700' },
})
