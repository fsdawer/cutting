package beauty.beauty.chat.dto;

import beauty.beauty.chat.entity.Message;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonDeserialize(builder = MessageResponse.MessageResponseBuilder.class)
public class MessageResponse {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String senderName;
    private String content;
    private String imageUrl;
    @JsonProperty("isRead")
    private boolean isRead;
    private LocalDateTime createdAt;

    @JsonPOJOBuilder(withPrefix = "")
    public static class MessageResponseBuilder {}

    public static MessageResponse from(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .roomId(message.getChatRoom().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .content(message.getContent())
                .imageUrl(message.getImageUrl())
                .isRead(message.isRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
