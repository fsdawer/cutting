import React, { useState, useEffect, useRef } from 'react'
import {
  View, Text, FlatList, TextInput, TouchableOpacity,
  StyleSheet, KeyboardAvoidingView, Platform, Alert,
} from 'react-native'
import useAuthStore from '../store/authStore'
import * as SecureStore from 'expo-secure-store'

const BASE_URL = 'http://localhost:8080'
const WS_URL = 'ws://localhost:8080/ws/websocket'

export default function ChatScreen() {
  const [rooms, setRooms] = useState([])
  const [activeRoom, setActiveRoom] = useState(null)
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const wsRef = useRef(null)
  const flatListRef = useRef(null)
  const user = useAuthStore((s) => s.user)

  useEffect(() => {
    fetchRooms()
    return () => wsRef.current?.close()
  }, [])

  async function fetchRooms() {
    const token = await SecureStore.getItemAsync('accessToken')
    try {
      const res = await fetch(`${BASE_URL}/api/chat/rooms`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      const data = await res.json()
      setRooms(data ?? [])
    } catch {
      Alert.alert('오류', '채팅 목록을 불러오지 못했습니다.')
    }
  }

  async function enterRoom(room) {
    setActiveRoom(room)
    const token = await SecureStore.getItemAsync('accessToken')
    try {
      const res = await fetch(`${BASE_URL}/api/chat/rooms/${room.id}/messages`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      const data = await res.json()
      setMessages(data ?? [])
    } catch {}
    connectWs(room.id, token)
  }

  function connectWs(roomId, token) {
    wsRef.current?.close()
    const ws = new WebSocket(`${WS_URL}?token=${token}`)
    ws.onopen = () => {
      ws.send(JSON.stringify({ command: 'SUBSCRIBE', destination: `/topic/chat/${roomId}` }))
    }
    ws.onmessage = (e) => {
      try {
        const msg = JSON.parse(e.data)
        setMessages((prev) => [...prev, msg])
        flatListRef.current?.scrollToEnd({ animated: true })
      } catch {}
    }
    wsRef.current = ws
  }

  async function sendMessage() {
    if (!input.trim() || !activeRoom) return
    const token = await SecureStore.getItemAsync('accessToken')
    try {
      await fetch(`${BASE_URL}/api/chat/rooms/${activeRoom.id}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ content: input.trim() }),
      })
      setInput('')
    } catch {}
  }

  if (activeRoom) {
    return (
      <KeyboardAvoidingView
        style={styles.container}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={90}
      >
        <View style={styles.chatHeader}>
          <TouchableOpacity onPress={() => setActiveRoom(null)}>
            <Text style={styles.back}>← 목록</Text>
          </TouchableOpacity>
          <Text style={styles.chatTitle}>{activeRoom.roomName ?? '채팅'}</Text>
        </View>

        <FlatList
          ref={flatListRef}
          data={messages}
          keyExtractor={(_, i) => String(i)}
          contentContainerStyle={styles.messageList}
          renderItem={({ item }) => {
            const isMine = item.senderId === user?.id
            return (
              <View style={[styles.bubble, isMine ? styles.bubbleMine : styles.bubbleOther]}>
                {!isMine && <Text style={styles.sender}>{item.senderName}</Text>}
                <Text style={[styles.messageText, isMine && styles.messageTextMine]}>
                  {item.content}
                </Text>
              </View>
            )
          }}
          onContentSizeChange={() => flatListRef.current?.scrollToEnd()}
        />

        <View style={styles.inputRow}>
          <TextInput
            style={styles.chatInput}
            value={input}
            onChangeText={setInput}
            placeholder="메시지를 입력하세요..."
            multiline
          />
          <TouchableOpacity style={styles.sendBtn} onPress={sendMessage}>
            <Text style={styles.sendText}>전송</Text>
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    )
  }

  return (
    <View style={styles.container}>
      <Text style={styles.pageTitle}>채팅</Text>
      {rooms.length === 0 ? (
        <Text style={styles.empty}>채팅방이 없습니다</Text>
      ) : (
        <FlatList
          data={rooms}
          keyExtractor={(item) => String(item.id)}
          renderItem={({ item }) => (
            <TouchableOpacity style={styles.roomCard} onPress={() => enterRoom(item)}>
              <Text style={styles.roomName}>{item.roomName ?? '채팅방'}</Text>
              <Text style={styles.roomLast} numberOfLines={1}>{item.lastMessage ?? ''}</Text>
            </TouchableOpacity>
          )}
        />
      )}
    </View>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8f9fa' },
  pageTitle: { fontSize: 20, fontWeight: '700', color: '#1a1a2e', padding: 16 },
  empty: { textAlign: 'center', color: '#aaa', marginTop: 60, fontSize: 14 },
  roomCard: {
    backgroundColor: '#fff', marginHorizontal: 16, marginBottom: 8,
    borderRadius: 12, padding: 16, shadowColor: '#000', shadowOpacity: 0.04,
    shadowRadius: 6, elevation: 1,
  },
  roomName: { fontSize: 15, fontWeight: '600', color: '#1a1a2e', marginBottom: 4 },
  roomLast: { fontSize: 13, color: '#888' },
  chatHeader: {
    flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff',
    padding: 14, borderBottomWidth: 1, borderBottomColor: '#eee', gap: 12,
  },
  back: { fontSize: 15, color: '#6c63ff' },
  chatTitle: { fontSize: 16, fontWeight: '700', color: '#1a1a2e' },
  messageList: { padding: 12, gap: 8 },
  bubble: { maxWidth: '75%', borderRadius: 12, padding: 10, marginBottom: 4 },
  bubbleMine: { backgroundColor: '#6c63ff', alignSelf: 'flex-end' },
  bubbleOther: { backgroundColor: '#fff', alignSelf: 'flex-start', borderWidth: 1, borderColor: '#eee' },
  sender: { fontSize: 11, color: '#888', marginBottom: 2 },
  messageText: { fontSize: 14, color: '#1a1a2e', lineHeight: 20 },
  messageTextMine: { color: '#fff' },
  inputRow: {
    flexDirection: 'row', padding: 10, backgroundColor: '#fff',
    borderTopWidth: 1, borderTopColor: '#eee', gap: 8, alignItems: 'flex-end',
  },
  chatInput: {
    flex: 1, borderWidth: 1.5, borderColor: '#ddd', borderRadius: 20,
    paddingHorizontal: 14, paddingVertical: 9, fontSize: 14, maxHeight: 100,
  },
  sendBtn: {
    backgroundColor: '#6c63ff', borderRadius: 20,
    paddingHorizontal: 16, paddingVertical: 10,
  },
  sendText: { color: '#fff', fontSize: 14, fontWeight: '600' },
})
