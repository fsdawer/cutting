package beauty.beauty.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {
    private Long roomId;
    private Long senderId;
    private String content;
    private String imageUrl;
}
