import { create } from 'zustand'
import * as SecureStore from 'expo-secure-store'

const useAuthStore = create((set) => ({
  user: null,
  accessToken: null,
  isLoggedIn: false,

  login: async (userData, tokens) => {
    await SecureStore.setItemAsync('accessToken', tokens.accessToken)
    if (tokens.refreshToken)
      await SecureStore.setItemAsync('refreshToken', tokens.refreshToken)
    set({ user: userData, accessToken: tokens.accessToken, isLoggedIn: true })
  },

  logout: async () => {
    await SecureStore.deleteItemAsync('accessToken')
    await SecureStore.deleteItemAsync('refreshToken')
    set({ user: null, accessToken: null, isLoggedIn: false })
  },

  restoreToken: async () => {
    const token = await SecureStore.getItemAsync('accessToken')
    if (token) set({ accessToken: token, isLoggedIn: true })
  },

  setUser: (user) => set({ user }),
}))

export default useAuthStore
