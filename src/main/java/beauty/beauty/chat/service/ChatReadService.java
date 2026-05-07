package beauty.beauty.chat.service;

import beauty.beauty.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 채팅 읽음 상태를 Redis에 저장해 DB 부하를 줄입니다.
 *
 * <pre>
 * 기존 방식 (DB is_read 컬럼):
 *   markAsRead   → UPDATE messages SET is_read=true WHERE room_id=? AND sender_id!=?  (N행 UPDATE)
 *   unreadCount  → SELECT COUNT(*) WHERE is_read=false AND sender_id!=?  (매 요청마다 집계)
 *
 * 개선 방식 (Redis lastread):
 *   markAsRead   → SET chat:lastread:{roomId}:{userId} = maxMessageId  (O(1) Redis 쓰기)
 *   unreadCount  → SELECT COUNT(*) WHERE id > lastReadId AND sender_id!=?
 *                  (PK 범위 인덱스 조건 → idx_msg_room_sender_id 활용)
 *
 * Redis 장애 시: DB is_read 컬럼 fallback (기존 로직 유지)
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class ChatReadService {

    private final StringRedisTemplate stringRedisTemplate;
    private final MessageRepository messageRepository;

    private static final String KEY_PREFIX = "chat:lastread:";

    /**
     * 읽음 처리: 해당 방의 최신 메시지 ID를 Redis에 저장.
     * DB UPDATE 없이 O(1)으로 처리.
     */
    public void markAsRead(Long roomId, Long userId) {
        Long latestId = messageRepository.findMaxIdByRoomId(roomId);
        if (latestId == null) return;
        stringRedisTemplate.opsForValue().set(buildKey(roomId, userId), String.valueOf(latestId));
    }

    /**
     * 미읽음 메시지 수 반환.
     * Redis에 lastReadId가 있으면 cursor 기반 COUNT (빠름),
     * 없으면 DB is_read 컬럼 fallback.
     */
    public long getUnreadCount(Long roomId, Long userId) {
        String lastReadStr = stringRedisTemplate.opsForValue().get(buildKey(roomId, userId));
        if (lastReadStr != null) {
            return messageRepository.countUnreadSince(roomId, userId, Long.parseLong(lastReadStr));
        }
        // Redis miss → DB fallback (초기 상태 or Redis 재시작 후)
        return messageRepository.countByChatRoomIdAndIsReadFalseAndSenderIdNot(roomId, userId);
    }

    private String buildKey(Long roomId, Long userId) {
        return KEY_PREFIX + roomId + ":" + userId;
    }
}
