package beauty.beauty.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateUserRequest {

    @Size(max = 50)
    private String name;            // 변경할 이름 (null이면 유지)

    @Size(max = 20)
    private String phone;           // 변경할 전화번호 (null이면 유지)

    @Size(max = 500)
    private String profileImg;      // 변경할 프로필 이미지 URL (null이면 유지)
}
