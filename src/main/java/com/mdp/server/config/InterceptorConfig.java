package com.mdp.server.config;

import com.mdp.server.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;

    public InterceptorConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/protectors/**") // 보호자 등록
                .addPathPatterns("/api/devices/**") // QR
                .addPathPatterns("/api/private/boards/**") // 게시판 추가 & 좋아요 추가
                .addPathPatterns("/api/private/comments") // 댓글 추가
                .addPathPatterns("/api/auth/private/guardians/status-check") // 보호자-피보호자 조회
                .excludePathPatterns("/api/auth/signup") // 로그인 제외
                .excludePathPatterns("/api/auth/login")// 회원가입 제외
                .excludePathPatterns("/api/alert/**") // 알림 제외
                .excludePathPatterns("/api/device/**"); // 센서 제어
    }
}