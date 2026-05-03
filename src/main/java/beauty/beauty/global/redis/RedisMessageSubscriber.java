package beauty.beauty.global.redis;

import beauty.beauty.chat.dto.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper redisObjectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String roomId  = channel.substring(channel.lastIndexOf(':') + 1);
            MessageResponse response = redisObjectMapper.readValue(message.getBody(), MessageResponse.class);
            messagingTemplate.convertAndSend("/topic/chat/" + roomId, response);
        } catch (JacksonException e) {
            log.error("Redis 채팅 메시지 처리 오류", e);
        }
    }
}
