package beauty.beauty.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러(Controller)에서 현재 인증된 사용자의 ID(Long)를 
 * 바로 파라미터로 주입받기 위한 커스텀 어노테이션.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUserId {
}
