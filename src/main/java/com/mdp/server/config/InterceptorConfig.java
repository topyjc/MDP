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
                .addPathPatterns("/api/devices/**") // api 검사
//              .addPathPatterns("/api/commit또는board/**) // 댓글 / 게시판 jWT 필요할 때 주석 빼셈
                .excludePathPatterns("/api/auth/**") // 로그인, 회원가입은 제외
                .excludePathPatterns("/api/alert/**")
                .excludePathPatterns("/api/device/**");
    }
}