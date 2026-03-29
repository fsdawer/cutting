package beauty.beauty.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;     // 현재 비밀번호 (본인 확인용)

    @NotBlank
    @Size(min = 8)
    private String newPassword;         // 새 비밀번호 (8자 이상)

    @NotBlank
    private String newPasswordConfirm;  // 새 비밀번호 확인 (newPassword와 동일해야 함)
}
