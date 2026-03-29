package beauty.beauty.global.resolver;

import beauty.beauty.global.annotation.LoginUserId;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class LoginUserIdResolver implements HandlerMethodArgumentResolver {

    // 이 리졸버가 처리할 파라미터 검사
    // 파라미터에 @LoginUserId 어노테이션이 붙어있고, 타입이 Long인지 확인
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasAnnotation = parameter.hasParameterAnnotation(LoginUserId.class);
        boolean isLongType = Long.class.isAssignableFrom(parameter.getParameterType());
        return hasAnnotation && isLongType;
    }

    // 실제로 바인딩할 데이터(유저 ID)를 리턴
    @Override
    public Object resolveArgument(MethodParameter parameter, 
                                  ModelAndViewContainer mavContainer, 
                                  NativeWebRequest webRequest, 
                                  WebDataBinderFactory binderFactory) throws Exception {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null 
            || authentication.getName().equals("anonymousUser")) {
            throw new IllegalStateException("인증 정보가 없습니다. 로그인이 필요합니다.");
        }

        return Long.parseLong(authentication.getName());
    }
}
