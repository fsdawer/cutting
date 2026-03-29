package beauty.beauty.stylist.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalTime;

/** GET 응답 — 영업시간 단건 정보 */
@Getter
@Builder
public class WorkingHoursResponse {

    private Long id;
    private int dayOfWeek;      // 0=월, 1=화, ..., 6=일
    private LocalTime openTime;
    private LocalTime closeTime;
    private boolean isDayOff;
}
