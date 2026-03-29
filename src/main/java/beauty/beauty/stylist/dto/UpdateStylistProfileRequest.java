package beauty.beauty.stylist.dto;

import lombok.Getter;

@Getter
public class UpdateStylistProfileRequest {
    
    // 미용사 본인이 수정할 수 있는 항목들
    private String salonName;
    private String location;
    private String bio;
    private Integer experience;
    
}
