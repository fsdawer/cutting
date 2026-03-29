package beauty.beauty.stylist.dto;

import lombok.Builder;
import lombok.Getter;

/** GET 응답 — 포트폴리오 이미지 단건 정보 */
@Getter
@Builder
public class PortfolioResponse {

    private Long id;
    private String imageUrl; // 이미지 URL (NCP Object Storage 등)
    private String caption;  // 설명 (선택)
}
