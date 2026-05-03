package beauty.beauty.stylist.controller;

import beauty.beauty.global.annotation.LoginUserId;
import beauty.beauty.stylist.dto.ServiceRequest;
import beauty.beauty.stylist.dto.ServiceResponse;
import beauty.beauty.stylist.dto.StylistProfileResponse;
import beauty.beauty.stylist.dto.UpdateStylistProfileRequest;
import beauty.beauty.stylist.dto.WorkingHoursRequest;
import beauty.beauty.stylist.dto.WorkingHoursResponse;
import beauty.beauty.stylist.service.StylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stylists")
@RequiredArgsConstructor
public class StylistController {

    private final StylistService stylistService;

    // GET  /api/stylists               전체 목록 (검색, 필터, 정렬)
    @GetMapping
    public ResponseEntity<List<StylistProfileResponse>> getStylists(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location) {

        return ResponseEntity.ok(stylistService.getStylists(keyword, location));
    }

    // GET  /api/stylists/{stylistId}    상세 조회
    @GetMapping("/{stylistId}")
    public ResponseEntity<StylistProfileResponse> getStylist(@PathVariable Long stylistId) {
        return ResponseEntity.ok(stylistService.getStylist(stylistId));
    }

    // GET  /api/stylists/me            내 프로필(미용사) 요약 조회
    @GetMapping("/me")
    public ResponseEntity<StylistProfileResponse> getMyProfile(@LoginUserId Long userId) {
        return ResponseEntity.ok(stylistService.getMyProfile(userId));
    }

    // PUT  /api/stylists/me            내 프로필 수정 (STYLIST 전용)
    @PutMapping("/me")
    public ResponseEntity<StylistProfileResponse> updateProfile(@LoginUserId Long userId,
                                                                @RequestBody UpdateStylistProfileRequest request) {
        return ResponseEntity.ok(stylistService.updateProfile(userId, request));
    }



    // ── 서비스 관리 ──────────────────────────────────────────────

    // POST   /api/stylists/me/services              서비스 추가
    @PostMapping("/me/services")
    public ResponseEntity<ServiceResponse> addService(@LoginUserId Long userId,
                                                      @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(stylistService.addService(userId, request));
    }

    // PUT    /api/stylists/me/services/{serviceId}  서비스 수정
    @PutMapping("/me/services/{serviceId}")
    public ResponseEntity<ServiceResponse> updateService(@LoginUserId Long userId,
                                                         @PathVariable Long serviceId,
                                                         @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(stylistService.updateService(userId, serviceId, request));
    }

    // DELETE /api/stylists/me/services/{serviceId}  서비스 삭제
    @DeleteMapping("/me/services/{serviceId}")
    public ResponseEntity<Void> deleteService(@LoginUserId Long userId,
                                              @PathVariable Long serviceId) {
        stylistService.deleteService(userId, serviceId);
        return ResponseEntity.ok().build();
    }



    // ── 영업시간 관리 ─────────────────────────────────────────────

    // PUT    /api/stylists/me/hours                영업시간 수정
    @PutMapping("/me/hours")
    public ResponseEntity<WorkingHoursResponse> updateHours(@LoginUserId Long userId,
                                                            @RequestBody WorkingHoursRequest request) {
        return ResponseEntity.ok(stylistService.updateHours(userId, request));
    }
}

