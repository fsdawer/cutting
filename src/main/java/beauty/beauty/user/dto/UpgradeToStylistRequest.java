package beauty.beauty.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UpgradeToStylistRequest {

    @NotBlank(message = "미용실 이름은 필수입니다.")
    private String salonName;

    @NotBlank(message = "근무 지역은 필수입니다.")
    private String location;
}
