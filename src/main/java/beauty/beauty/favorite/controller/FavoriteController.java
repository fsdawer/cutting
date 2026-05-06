package beauty.beauty.favorite.controller;

import beauty.beauty.favorite.service.FavoriteService;
import beauty.beauty.global.annotation.LoginUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/stylists/{stylistProfileId}")
    public ResponseEntity<String> toggleFavorite(
            @LoginUserId Long userId,
            @PathVariable Long stylistProfileId) {

        boolean isFavorited = favoriteService.toggleFavorite(userId, stylistProfileId);

        if (isFavorited) {
            return ResponseEntity.ok("찜하기가 완료되었습니다.");
        } else {
            return ResponseEntity.ok("찜하기가 취소되었습니다.");
        }
    }
}
