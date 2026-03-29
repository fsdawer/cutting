package beauty.beauty.stylist.service;

import beauty.beauty.stylist.dto.ServiceRequest;
import beauty.beauty.stylist.dto.ServiceResponse;
import beauty.beauty.stylist.dto.WorkingHoursRequest;
import beauty.beauty.stylist.dto.WorkingHoursResponse;
import beauty.beauty.stylist.entity.OperatingHours;
import beauty.beauty.stylist.entity.StylistProfile;
import beauty.beauty.stylist.entity.StylistServiceItem;
import beauty.beauty.stylist.repository.OperatingHoursRepository;
import beauty.beauty.stylist.repository.StylistProfileRepository;
import beauty.beauty.stylist.repository.StylistServiceRepository;
import beauty.beauty.user.entity.User;
import beauty.beauty.user.entity.User.Role;
import beauty.beauty.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test") // H2 등 테스트용 DB를 사용하기 위함
class StylistServiceImplIntegrationTest {

    @Autowired
    private StylistServiceImpl stylistService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StylistProfileRepository stylistProfileRepository;

    @Autowired
    private StylistServiceRepository stylistServiceRepository;

    @Autowired
    private OperatingHoursRepository operatingHoursRepository;

    private User testUser;
    private StylistProfile testProfile;

    @BeforeEach
    void setUp() {
        // 1. 테스트용 유저 생성 (unique constraint 해결을 위해 랜덤값 추가)
        String randomSuffix = String.valueOf(System.currentTimeMillis());
        testUser = User.builder()
                .username("test_user_" + randomSuffix)
                .email("teststylist_" + randomSuffix + "@beauty.com")
                .password("test1234")
                .name("테스트 미용사")
                .phone("010-1234-5678")
                .role(Role.STYLIST)
                .build();
        userRepository.save(testUser);

        // 2. 테스트용 미용사 프로필 생성
        testProfile = StylistProfile.builder()
                .user(testUser)
                .salonName("테스트 미용실")
                .location("테스트시 테스트동")
                .experience(5)
                .build();
        stylistProfileRepository.save(testProfile);
    }

    @Test
    @DisplayName("미용사 서비스 추가 통합 테스트")
    void addService() {
        // given
        ServiceRequest request = ServiceRequest.builder()
                .name("커트")
                .price(20000)
                .durationMinutes(40)
                .description("남성 커트")
                .build();

        // when
        ServiceResponse response = stylistService.addService(testUser.getId(), request);

        // then
        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("커트");
        assertThat(response.getPrice()).isEqualTo(20000);
        assertThat(response.getDurationMinutes()).isEqualTo(40);
        
        List<StylistServiceItem> items = stylistServiceRepository.findByStylistProfileIdAndIsActiveTrue(testProfile.getId());
        assertThat(items).hasSize(1);
    }

    @Test
    @DisplayName("미용사 서비스 수정 통합 테스트")
    void updateService() {
        // given: 먼저 서비스 하나 추가
        ServiceRequest addReq = ServiceRequest.builder()
                .name("펌")
                .price(80000)
                .durationMinutes(90)
                .build();
        ServiceResponse addedService = stylistService.addService(testUser.getId(), addReq);

        // when: 수정 요청
        ServiceRequest updateReq = ServiceRequest.builder()
                .name("수정된 펌")
                .price(90000)
                .durationMinutes(100)
                .build();
        ServiceResponse updated = stylistService.updateService(testUser.getId(), addedService.getId(), updateReq);

        // then
        assertThat(updated.getName()).isEqualTo("수정된 펌");
        assertThat(updated.getPrice()).isEqualTo(90000);
        
        StylistServiceItem dbItem = stylistServiceRepository.findById(addedService.getId()).orElseThrow();
        assertThat(dbItem.getName()).isEqualTo("수정된 펌");
        assertThat(dbItem.getPrice()).isEqualTo(90000);
    }

    @Test
    @DisplayName("미용사 서비스 삭제 통합 테스트 (Soft Delete)")
    void deleteService() {
        // given
        ServiceRequest addReq = ServiceRequest.builder()
                .name("염색")
                .price(50000)
                .durationMinutes(60)
                .build();
        ServiceResponse addedService = stylistService.addService(testUser.getId(), addReq);

        // when
        stylistService.deleteService(testUser.getId(), addedService.getId());

        // then: findByStylistProfileIdAndIsActiveTrue 조회 시 안 나와야 함
        List<StylistServiceItem> activeItems = stylistServiceRepository.findByStylistProfileIdAndIsActiveTrue(testProfile.getId());
        assertThat(activeItems).isEmpty();

        // 실제로 DB에는 남아있고 isActive만 false 여야 함
        StylistServiceItem dbItem = stylistServiceRepository.findById(addedService.getId()).orElseThrow();
        assertThat(dbItem.isActive()).isFalse();
    }

    @Test
    @DisplayName("영업시간 추가/수정 통합(Upsert) 테스트")
    void updateHours() {
        // given: 월요일(0) 처음 세팅
        WorkingHoursRequest request1 = WorkingHoursRequest.builder()
                .dayOfWeek(0)
                .openTime(LocalTime.of(10, 0))
                .closeTime(LocalTime.of(20, 0))
                .isDayOff(false)
                .build();

        // when 1: Insert
        WorkingHoursResponse res1 = stylistService.updateHours(testUser.getId(), request1);

        // then 1
        assertThat(res1.getId()).isNotNull();
        assertThat(res1.getOpenTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(res1.isDayOff()).isFalse();

        // when 2: Update (같은 요일 오픈시간 변경)
        WorkingHoursRequest request2 = WorkingHoursRequest.builder()
                .dayOfWeek(0)
                .openTime(LocalTime.of(11, 0))
                .closeTime(LocalTime.of(21, 0))
                .isDayOff(false)
                .build();
        WorkingHoursResponse res2 = stylistService.updateHours(testUser.getId(), request2);

        // then 2
        assertThat(res2.getId()).isEqualTo(res1.getId()); // 같은 ID여야 함 (Upsert)
        assertThat(res2.getOpenTime()).isEqualTo(LocalTime.of(11, 0));

        // DB 검증
        List<OperatingHours> hoursList = operatingHoursRepository.findByStylistProfileId(testProfile.getId());
        assertThat(hoursList).hasSize(1);
    }
}
