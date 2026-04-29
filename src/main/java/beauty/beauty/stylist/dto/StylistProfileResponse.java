package beauty.beauty.stylist.dto;

import beauty.beauty.stylist.entity.Salon;
import beauty.beauty.stylist.entity.StylistProfile;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class StylistProfileResponse {

    private Long id;
    private Long userId;
    private String name;
    private String profileImg;
    private String bio;
    private int experience;
    private BigDecimal rating;
    private int reviewCount;

    // 소속 미용실 정보
    private String salonName;
    private String location;         // 미용실 주소 (프론트 호환 유지)
    private String salonPhone;
    private String salonDescription;

    private List<ServiceResponse> services;
    private List<WorkingHoursResponse> workingHours;
    private List<PortfolioResponse> portfolios;

    public static StylistProfileResponse from(StylistProfile profile) {
        Salon salon = profile.getSalon();
        return StylistProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .name(profile.getUser().getName())
                .profileImg(profile.getUser().getProfileImg())
                .bio(profile.getBio())
                .experience(profile.getExperience())
                .rating(profile.getRating())
                .reviewCount(profile.getReviewCount())
                .salonName(salon != null ? salon.getName() : null)
                .location(salon != null ? salon.getAddress() : null)
                .salonPhone(salon != null ? salon.getPhone() : null)
                .salonDescription(salon != null ? salon.getDescription() : null)
                .build();
    }

    public static StylistProfileResponse fromWithDetails(StylistProfile profile,
                                              List<ServiceResponse> services,
                                              List<WorkingHoursResponse> workingHours,
                                              List<PortfolioResponse> portfolios) {
        Salon salon = profile.getSalon();
        return StylistProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .name(profile.getUser().getName())
                .profileImg(profile.getUser().getProfileImg())
                .bio(profile.getBio())
                .experience(profile.getExperience())
                .rating(profile.getRating())
                .reviewCount(profile.getReviewCount())
                .salonName(salon != null ? salon.getName() : null)
                .location(salon != null ? salon.getAddress() : null)
                .salonPhone(salon != null ? salon.getPhone() : null)
                .salonDescription(salon != null ? salon.getDescription() : null)
                .services(services)
                .workingHours(workingHours)
                .portfolios(portfolios)
                .build();
    }
}
