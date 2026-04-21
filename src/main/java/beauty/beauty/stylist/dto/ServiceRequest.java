package beauty.beauty.stylist.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/** POST /api/stylists/me/services — 서비스 추가 요청 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {

    @NotBlank
    private String name;        // 서비스명 (예: 커트, 펌, 염색)

    private String category;    // 카테고리 (예: 커트, 펌, 염색, 케어, 기타)

    private String description; // 서비스 설명

    @Min(0)
    private int price;          // 가격 (원)

    @Min(1)
    private int durationMinutes; // 소요 시간 (분)
}
