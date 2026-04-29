package beauty.beauty.stylist.dto;

import lombok.Getter;

@Getter
public class UpdateStylistProfileRequest {

    // 미용사 본인 정보
    private String bio;
    private Integer experience;

    // 소속 미용실 정보
    private String salonName;
    private String location;        // 미용실 주소
    private String salonPhone;
    private String salonDescription;
}
