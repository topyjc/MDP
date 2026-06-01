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
                .addPathPatterns("/api/private/boards") // 게시판 추가
                .addPathPatterns("/api/private/comments") // 댓글 추가
                .excludePathPatterns("/api/auth/**") // 로그인, 회원가입 제외
                .excludePathPatterns("/api/alert/**") // 알림 제외
                .excludePathPatterns("/api/device/**"); // 센서 제어
    }
}