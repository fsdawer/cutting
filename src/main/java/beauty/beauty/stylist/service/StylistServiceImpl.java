package beauty.beauty.stylist.service;

import beauty.beauty.stylist.dto.*;
import beauty.beauty.stylist.entity.OperatingHours;
import beauty.beauty.stylist.entity.StylistProfile;
import beauty.beauty.stylist.entity.StylistServiceItem;
import beauty.beauty.stylist.repository.OperatingHoursRepository;
import beauty.beauty.stylist.repository.PortfolioRepository;
import beauty.beauty.stylist.repository.StylistProfileRepository;
import beauty.beauty.stylist.repository.StylistServiceRepository;
import beauty.beauty.user.entity.User;
import beauty.beauty.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StylistServiceImpl implements StylistService{

    private final StylistProfileRepository stylistProfileRepository;
    private final StylistServiceRepository stylistServiceRepository;
    private final OperatingHoursRepository operatingHoursRepository;
    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;


    // ----- 전체 목록(스타일리스트) 조회 ------
    @Override
    public List<StylistProfileResponse> getStylists(String keyword,
                                                    String location) {
        List<StylistProfile> stylists = stylistProfileRepository.searchStylists(keyword, location);
        return stylists.stream().map(StylistProfileResponse::from).collect(Collectors.toList());
    }


    // ----- 스타일리스트 상세 조회 ------
    @Override
    public StylistProfileResponse getStylist(Long stylistId) {
        StylistProfile profile = stylistProfileRepository.findById(stylistId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 미용사 프로필입니다. id=" + stylistId));
        return StylistProfileResponse.from(profile);
    }

    // ----- 내 미용사 프로필 조회 ------
    @Override
    public StylistProfileResponse getMyProfile(Long userId) {
        StylistProfile profile = stylistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저의 미용사 프로필을 찾을 수 없습니다."));
        return StylistProfileResponse.from(profile);
    }


    // ----- 스타일리스트 프로필 업데이트 ------
    @Override
    public StylistProfileResponse updateProfile(Long userId,
                                                UpdateStylistProfileRequest request) {
        
        // 1. userId로 미용사 프로필 찾기
        StylistProfile profile = stylistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저의 미용사 프로필을 찾을 수 없습니다."));

        // 2. 요청으로 들어온 값들만(null이 아닌 경우) 업데이트
        if (request.getSalonName() != null) {
            profile.setSalonName(request.getSalonName());
        }
        if (request.getLocation() != null) {
            profile.setLocation(request.getLocation());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getExperience() != null) {
            profile.setExperience(request.getExperience());
        }

        // 3. 업데이트된 프로필 저장
        stylistProfileRepository.save(profile);

        // 4. 응답 DTO로 변환하여 반환
        return StylistProfileResponse.from(profile);
    }


    // ----- 스타일리스트 서비스 추가 ------
    @Override
    public ServiceResponse addService(Long userId, ServiceRequest request) {
        // 1. userId로 미용사 프로필 찾기
        StylistProfile profile = stylistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저의 미용사 프로필을 찾을 수 없습니다."));


        StylistServiceItem serviceItem = StylistServiceItem.builder()
                .stylistProfile(profile)
                .name(request.getName())
                .price(request.getPrice())
                .duration(request.getDurationMinutes())
                .description(request.getDescription())
                .isActive(true)
                .build();

        StylistServiceItem savedItem = stylistServiceRepository.save(serviceItem);

        // 4. 저장된 엔티티를 반환용 DTO(ServiceResponse)로 변환하여 응답
        return ServiceResponse.builder()
                .id(savedItem.getId())
                .name(savedItem.getName())
                .price(savedItem.getPrice())
                .durationMinutes(savedItem.getDuration())
                .description(savedItem.getDescription())
                .build();
    }


    // ----- 스타일리스트 서비스 수정 ------
    @Override
    public ServiceResponse updateService(Long userId, Long serviceId, ServiceRequest request) {

        // 1. userId로 미용사 프로필 찾기
        StylistProfile profile = stylistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저의 미용사 프로필을 찾을 수 없습니다."));

        // 2. serviceId로 수정할 기존 서비스 항목 찾기
        StylistServiceItem serviceItem = stylistServiceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 서비스입니다. id=" + serviceId));

        // 3. 권한 체크: 해당 서비스가 현재 로그인한 미용사의 서비스가 맞는지 확인
        if (!serviceItem.getStylistProfile().getId().equals(profile.getId())) {
            throw new IllegalArgumentException("해당 서비스를 수정할 권한이 없습니다.");
        }

        // 4. 엔티티 내부에 구현된 update 메서드를 호출하여if문을 숨김 (도메인 캡슐화)
        serviceItem.update(
                request.getName(),
                request.getPrice(),
                request.getDurationMinutes(),
                request.getDescription()
        );

        // 5. 업데이트 내용 저장
        StylistServiceItem updatedItem = stylistServiceRepository.save(serviceItem);
        // 6. 응답 DTO로 변환하여 반환
        return ServiceResponse.builder()
                .id(updatedItem.getId())
                .name(updatedItem.getName())
                .price(updatedItem.getPrice())
                .durationMinutes(updatedItem.getDuration())
                .description(updatedItem.getDescription())
                .build();
    }



    // ----- 스타일리스트 서비스 삭제 ------
    @Override
    public void deleteService(Long userId, Long serviceId) {

        // 1. userId로 미용사 프로필 찾기
        StylistProfile profile = stylistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저의 미용사 프로필을 찾을 수 없습니다."));

        // 2. serviceId로 삭제할 기존 서비스 항목 찾기
        StylistServiceItem serviceItem = stylistServiceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 서비스입니다. id=" + serviceId));

        // 3. 권한 체크: 해당 서비스가 현재 로그인한 미용사의 서비스가 맞는지 확인
        if (!serviceItem.getStylistProfile().getId().equals(profile.getId())) {
            throw new IllegalArgumentException("해당 서비스를 수정할 권한이 없습니다.");
        }

        // 4. 소프트 삭제 처리 (isActive 상태를 false로 변경)
        serviceItem.setActive(false); 
        stylistServiceRepository.save(serviceItem);
    }


    @Override
    public PortfolioResponse addPortfolio(Long userId, String imageUrl, String caption) {
        return null;
    }


    @Override
    public void deletePortfolio(Long userId, Long portfolioId) {

    }


    // ----- 운영시간 수정 ------
    @Override
    public WorkingHoursResponse updateHours(Long userId, WorkingHoursRequest request) {
        StylistProfile profile = stylistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저의 미용사 프로필을 찾을 수 없습니다."));

        // 2. 해당 요일의 운영시간 데이터가 존재하는지 확인
        OperatingHours operatingHours = operatingHoursRepository
                .findByStylistProfileIdAndDayOfWeek(profile.getId(), request.getDayOfWeek())
                .orElse(null); // 없으면 null 반환

        if (operatingHours == null) {
            // 3-A. 데이터가 없으면 새로 생성 (등록 로직)
            operatingHours = OperatingHours.builder()
                    .stylistProfile(profile)
                    .dayOfWeek(request.getDayOfWeek())
                    .openTime(request.getOpenTime())
                    .closeTime(request.getCloseTime())
                    .isClosed(request.isDayOff())
                    .build();
        } else {
            // 3-B. 데이터가 있으면 시간만 수정 (수정 로직)
            operatingHours.update(request.getOpenTime(), request.getCloseTime(), request.isDayOff());
        }

        // 4. DB 저장
        OperatingHours savedHours = operatingHoursRepository.save(operatingHours);

        // 5. 응답 반환
        return WorkingHoursResponse.builder()
                .id(savedHours.getId())
                .dayOfWeek(savedHours.getDayOfWeek())
                .openTime(savedHours.getOpenTime())
                .closeTime(savedHours.getCloseTime())
                .isDayOff(savedHours.isClosed())
                .build();
    }



    // ID 조회 공통 메서드
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 유저입니다. id=" + userId));
    }

}
