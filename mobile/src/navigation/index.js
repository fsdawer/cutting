import React from 'react'
import { NavigationContainer } from '@react-navigation/native'
import { createStackNavigator } from '@react-navigation/stack'
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs'
import { Text } from 'react-native'
import useAuthStore from '../store/authStore'

import HomeScreen from '../screens/HomeScreen'
import StylistDetailScreen from '../screens/StylistDetailScreen'
import BookingScreen from '../screens/BookingScreen'
import PaymentScreen from '../screens/PaymentScreen'
import LoginScreen from '../screens/LoginScreen'
import RegisterScreen from '../screens/RegisterScreen'
import MyPageScreen from '../screens/MyPageScreen'
import StylistManageScreen from '../screens/StylistManageScreen'
import ChatScreen from '../screens/ChatScreen'

const Stack = createStackNavigator()
const Tab = createBottomTabNavigator()

function HomeTabs() {
  return (
    <Tab.Navigator screenOptions={{ headerShown: false, tabBarActiveTintColor: '#6c63ff' }}>
      <Tab.Screen
        name="홈"
        component={HomeScreen}
        options={{ tabBarIcon: ({ color }) => <Text style={{ fontSize: 20, color }}>🏠</Text> }}
      />
      <Tab.Screen
        name="채팅"
        component={ChatScreen}
        options={{ tabBarIcon: ({ color }) => <Text style={{ fontSize: 20, color }}>💬</Text> }}
      />
      <Tab.Screen
        name="마이페이지"
        component={MyPageScreen}
        options={{ tabBarIcon: ({ color }) => <Text style={{ fontSize: 20, color }}>👤</Text> }}
      />
    </Tab.Navigator>
  )
}

export default function AppNavigator() {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn)

  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {isLoggedIn ? (
          <>
            <Stack.Screen name="Main" component={HomeTabs} />
            <Stack.Screen name="StylistDetail" component={StylistDetailScreen}
              options={{ headerShown: true, title: '미용사 상세' }} />
            <Stack.Screen name="Booking" component={BookingScreen}
              options={{ headerShown: true, title: '예약하기' }} />
            <Stack.Screen name="Payment" component={PaymentScreen}
              options={{ headerShown: true, title: '결제' }} />
            <Stack.Screen name="StylistManage" component={StylistManageScreen}
              options={{ headerShown: true, title: '프로필 관리' }} />
          </>
        ) : (
          <>
            <Stack.Screen name="Login" component={LoginScreen} />
            <Stack.Screen name="Register" component={RegisterScreen}
              options={{ headerShown: true, title: '회원가입' }} />
          </>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  )
}
