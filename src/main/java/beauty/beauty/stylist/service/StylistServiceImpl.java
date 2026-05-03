package beauty.beauty.stylist.service;

import beauty.beauty.stylist.dto.*;
import beauty.beauty.stylist.entity.OperatingHours;
import beauty.beauty.stylist.entity.Salon;
import beauty.beauty.stylist.entity.StylistProfile;
import beauty.beauty.stylist.entity.StylistServiceItem;
import beauty.beauty.stylist.repository.OperatingHoursRepository;
import beauty.beauty.stylist.repository.SalonRepository;
import beauty.beauty.stylist.repository.StylistProfileRepository;
import beauty.beauty.stylist.repository.StylistServiceRepository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StylistServiceImpl implements StylistService {

    private final StylistProfileRepository stylistProfileRepository;
    private final StylistServiceRepository stylistServiceRepository;
    private final OperatingHoursRepository operatingHoursRepository;
    private final SalonRepository salonRepository;

    @Override
    public List<StylistProfileResponse> getStylists(String keyword, String location) {
        return stylistProfileRepository.searchStylists(keyword, location)
                .stream().map(StylistProfileResponse::from).collect(Collectors.toList());
    }

    @Override
    public StylistProfileResponse getStylist(Long stylistId) {
        StylistProfile profile = stylistProfileRepository.findById(stylistId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 미용사 프로필입니다. id=" + stylistId));
        return getDetailedProfileResponse(profile);
    }

    @Override
    public StylistProfileResponse getMyProfile(Long userId) {
        StylistProfile profile = stylistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저의 미용사 프로필을 찾을 수 없습니다."));
        return getDetailedProfileResponse(profile);
    }

    private StylistProfileResponse getDetailedProfileResponse(StylistProfile profile) {
        List<ServiceResponse> services = stylistServiceRepository
                .findByStylistProfileIdAndIsActiveTrue(profile.getId())
                .stream().map(s -> ServiceResponse.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .category(s.getCategory())
                        .description(s.getDescription())
                        .price(s.getPrice())
                        .durationMinutes(s.getDuration())
                        .build())
                .collect(Collectors.toList());

        List<WorkingHoursResponse> workingHours = operatingHoursRepository
                .findByStylistProfileId(profile.getId())
                .stream().map(h -> WorkingHoursResponse.builder()
                        .id(h.getId())
                        .dayOfWeek(h.getDayOfWeek())
                        .openTime(h.getOpenTime())
                        .closeTime(h.getCloseTime())
                        .isDayOff(h.isClosed())
                        .build())
                .collect(Collectors.toList());

        return StylistProfileResponse.fromWithDetails(profile, services, workingHours);
    }

    @Override
    @Transactional
    public StylistProfileResponse updateProfile(Long userId, UpdateStylistProfileRequest request) {
        StylistProfile profile = stylistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저의 미용사 프로필을 찾을 수 없습니다."));

        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getExperience() != null) profile.setExperience(request.getExperience());

        Salon salon = profile.getSalon();
        if (salon == null) {
            salon = salonRepository.save(new Salon());
            profile.setSalon(salon);
        }
        if (request.getSalonName() != null) salon.setName(request.getSalonName());
        if (request.getLocation() != null) salon.setAddress(request.getLocation());
        if (request.getSalonPhone() != null) salon.setPhone(request.getSalonPhone());
        if (request.getSalonDescription() != null) salon.setDescription(request.getSalonDescription());

        return StylistProfileResponse.from(profile);
    }

    @Override
    public ServiceResponse addService(Long userId, ServiceRequest request) {
        StylistProfile profile = stylistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저의 미용사 프로필을 찾을 수 없습니다."));

        StylistServiceItem serviceItem = StylistServiceItem.builder()
                .stylistProfile(profile)
                .name(request.getName())
                .category(request.getCategory())
                .price(request.getPrice())
                .duration(request.getDurationMinutes())
                .description(request.getDescription())
                .build();

        StylistServiceItem saved = stylistServiceRepository.save(serviceItem);

        return ServiceResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .category(saved.getCategory())
                .price(saved.getPrice())
                .durationMinutes(saved.getDuration())
                .description(saved.getDescription())
                .build();
    }

    @Override
    public ServiceResponse updateService(Long userId, Long serviceId, ServiceRequest request) {
        StylistProfile profile = stylistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저의 미용사 프로필을 찾을 수 없습니다."));

        StylistServiceItem serviceItem = stylistServiceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 서비스입니다. id=" + serviceId));

        if (!serviceItem.getStylistProfile().getId().equals(profile.getId())) {
            throw new IllegalArgumentException("해당 서비스를 수정할 권한이 없습니다.");
        }

        serviceItem.update(
                request.getName(),
                request.getCategory(),
                request.getPrice(),
                request.getDurationMinutes(),
                request.getDescription()
        );

        StylistServiceItem updated = stylistServiceRepository.save(serviceItem);

        return ServiceResponse.builder()
                .id(updated.getId())
                .name(updated.getName())
                .category(updated.getCategory())
                .price(updated.getPrice())
                .durationMinutes(updated.getDuration())
                .description(updated.getDescription())
                .build();
    }

    @Override
    public void deleteService(Long userId, Long serviceId) {
        StylistProfile profile = stylistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저의 미용사 프로필을 찾을 수 없습니다."));

        StylistServiceItem serviceItem = stylistServiceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 서비스입니다. id=" + serviceId));

        if (!serviceItem.getStylistProfile().getId().equals(profile.getId())) {
            throw new IllegalArgumentException("해당 서비스를 수정할 권한이 없습니다.");
        }

        serviceItem.setActive(false);
        stylistServiceRepository.save(serviceItem);
    }

    @Override
    public WorkingHoursResponse updateHours(Long userId, WorkingHoursRequest request) {
        StylistProfile profile = stylistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저의 미용사 프로필을 찾을 수 없습니다."));

        OperatingHours operatingHours = operatingHoursRepository
                .findByStylistProfileIdAndDayOfWeek(profile.getId(), request.getDayOfWeek())
                .orElse(null);

        if (operatingHours == null) {
            operatingHours = OperatingHours.builder()
                    .stylistProfile(profile)
                    .dayOfWeek(request.getDayOfWeek())
                    .openTime(request.getOpenTime())
                    .closeTime(request.getCloseTime())
                    .isClosed(request.isDayOff())
                    .build();
        } else {
            operatingHours.update(request.getOpenTime(), request.getCloseTime(), request.isDayOff());
        }

        OperatingHours saved = operatingHoursRepository.save(operatingHours);

        return WorkingHoursResponse.builder()
                .id(saved.getId())
                .dayOfWeek(saved.getDayOfWeek())
                .openTime(saved.getOpenTime())
                .closeTime(saved.getCloseTime())
                .isDayOff(saved.isClosed())
                .build();
    }
}
