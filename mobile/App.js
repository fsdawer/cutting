import React, { useEffect } from 'react'
import { GestureHandlerRootView } from 'react-native-gesture-handler'
import AppNavigator from './src/navigation'
import useAuthStore from './src/store/authStore'

export default function App() {
  const restoreToken = useAuthStore((s) => s.restoreToken)

  useEffect(() => { restoreToken() }, [])

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <AppNavigator />
    </GestureHandlerRootView>
  )
}
