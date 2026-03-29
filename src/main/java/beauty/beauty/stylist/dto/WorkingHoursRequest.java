package beauty.beauty.stylist.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/** PUT /api/stylists/me/hours — 영업시간 수정 요청 (요일 하나 단위) */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkingHoursRequest {

    @NotNull
    @Min(0) @Max(6)
    private Integer dayOfWeek; // 0=월, 1=화, ..., 6=일

    private LocalTime openTime;  // 영업 시작 시간 (null이면 휴무)
    private LocalTime closeTime; // 영업 종료 시간 (null이면 휴무)
    private boolean isDayOff;    // 휴무 여부
}
