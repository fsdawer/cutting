package beauty.beauty.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequest {

    @NotBlank
    private String username;   // 아이디 또는 이메일

    @NotBlank
    private String password;
}
