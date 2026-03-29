package beauty.beauty.stylist.dto;

import lombok.Builder;
import lombok.Getter;

/** GET 응답 — 서비스 단건 정보 */
@Getter
@Builder
public class ServiceResponse {

    private Long id;
    private String name;
    private String description;
    private int price;
    private int durationMinutes;
}
