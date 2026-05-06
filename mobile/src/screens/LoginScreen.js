import React, { useState } from 'react'
import {
  View, Text, TextInput, TouchableOpacity,
  StyleSheet, Alert, KeyboardAvoidingView, Platform,
} from 'react-native'
import { authApi } from '../api/auth'
import useAuthStore from '../store/authStore'

export default function LoginScreen({ navigation }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const login = useAuthStore((s) => s.login)

  async function handleLogin() {
    if (!username || !password) {
      Alert.alert('입력 오류', '아이디와 비밀번호를 입력해주세요.')
      return
    }
    setLoading(true)
    try {
      const res = await authApi.login({ username, password })
      const { accessToken, refreshToken, ...user } = res.data
      await login(user, { accessToken, refreshToken })
    } catch (e) {
      Alert.alert('로그인 실패', e.response?.data?.message ?? '아이디 또는 비밀번호를 확인해주세요.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <Text style={styles.logo}>CutIng</Text>
      <Text style={styles.sub}>믿을 수 있는 스타일리스트 예약 플랫폼</Text>

      <View style={styles.form}>
        <TextInput
          style={styles.input}
          placeholder="아이디"
          value={username}
          onChangeText={setUsername}
          autoCapitalize="none"
        />
        <TextInput
          style={styles.input}
          placeholder="비밀번호"
          value={password}
          onChangeText={setPassword}
          secureTextEntry
        />
        <TouchableOpacity
          style={[styles.btn, loading && styles.btnDisabled]}
          onPress={handleLogin}
          disabled={loading}
        >
          <Text style={styles.btnText}>{loading ? '로그인 중...' : '로그인'}</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.link} onPress={() => navigation.navigate('Register')}>
          <Text style={styles.linkText}>아직 계정이 없으신가요? <Text style={styles.linkBold}>회원가입</Text></Text>
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', backgroundColor: '#fff', paddingHorizontal: 28 },
  logo: { fontSize: 36, fontWeight: '800', color: '#6c63ff', textAlign: 'center', marginBottom: 8 },
  sub: { fontSize: 14, color: '#888', textAlign: 'center', marginBottom: 40 },
  form: { gap: 14 },
  input: {
    borderWidth: 1.5, borderColor: '#ddd', borderRadius: 10,
    paddingHorizontal: 16, paddingVertical: 13, fontSize: 15,
  },
  btn: {
    backgroundColor: '#6c63ff', borderRadius: 10,
    paddingVertical: 14, alignItems: 'center',
  },
  btnDisabled: { opacity: 0.6 },
  btnText: { color: '#fff', fontSize: 16, fontWeight: '700' },
  link: { alignItems: 'center', marginTop: 4 },
  linkText: { fontSize: 14, color: '#888' },
  linkBold: { color: '#6c63ff', fontWeight: '600' },
})
