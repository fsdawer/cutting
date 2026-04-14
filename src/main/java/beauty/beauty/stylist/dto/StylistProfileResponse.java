package beauty.beauty.stylist.dto;

import beauty.beauty.stylist.entity.StylistProfile;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class StylistProfileResponse {

    private Long id;
    private Long userId;
    private String name;        // User 엔티티에서 가져옴
    private String profileImg;  // User 엔티티에서 가져옴
    private String salonName;
    private String location;
    private String bio;
    private int experience;
    private double rating;
    private int reviewCount;

    private List<ServiceResponse> services;
    private List<WorkingHoursResponse> workingHours;
    private List<PortfolioResponse> portfolios;

    public static StylistProfileResponse from(StylistProfile profile) {
        return StylistProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .name(profile.getUser().getName())
                .profileImg(profile.getUser().getProfileImg())
                .salonName(profile.getSalonName())
                .location(profile.getLocation())
                .bio(profile.getBio())
                .experience(profile.getExperience())
                .rating(profile.getRating())
                .reviewCount(profile.getReviewCount())
                .build();
    }

    public static StylistProfileResponse fromWithDetails(StylistProfile profile,
                                              List<ServiceResponse> services,
                                              List<WorkingHoursResponse> workingHours,
                                              List<PortfolioResponse> portfolios) {
        return StylistProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .name(profile.getUser().getName())
                .profileImg(profile.getUser().getProfileImg())
                .salonName(profile.getSalonName())
                .location(profile.getLocation())
                .bio(profile.getBio())
                .experience(profile.getExperience())
                .rating(profile.getRating())
                .reviewCount(profile.getReviewCount())
                .services(services)
                .workingHours(workingHours)
                .portfolios(portfolios)
                .build();
    }
}
