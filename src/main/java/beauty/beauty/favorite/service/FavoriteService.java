package beauty.beauty.favorite.service;

import beauty.beauty.favorite.entity.FavoriteStylist;
import beauty.beauty.favorite.repository.FavoriteStylistRepository;
import beauty.beauty.stylist.dto.StylistProfileResponse;
import beauty.beauty.stylist.entity.StylistProfile;
import beauty.beauty.stylist.repository.StylistProfileRepository;
import beauty.beauty.user.entity.User;
import beauty.beauty.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteStylistRepository favoriteStylistRepository;
    private final StylistProfileRepository stylistProfileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<StylistProfileResponse> getMyFavorites(Long userId) {
        return favoriteStylistRepository.findByUserIdWithDetails(userId)
                .stream()
                .map(fs -> StylistProfileResponse.from(fs.getStylistProfile()))
                .toList();
    }

    @Transactional
    public boolean toggleFavorite(Long userId, Long stylistProfileId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        StylistProfile stylistProfile = stylistProfileRepository.findById(stylistProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Stylist not found"));

        var existing = favoriteStylistRepository.findByUserIdAndStylistProfileId(userId, stylistProfileId);

        if (existing.isPresent()) {
            favoriteStylistRepository.delete(existing.get());
            stylistProfileRepository.decrementFavoriteCount(stylistProfileId);
            return false;
        } else {
            FavoriteStylist favoriteStylist = FavoriteStylist.builder()
                    .user(user)
                    .stylistProfile(stylistProfile)
                    .build();
            favoriteStylistRepository.save(favoriteStylist);
            stylistProfileRepository.incrementFavoriteCount(stylistProfileId);
            return true;
        }
    }
}
