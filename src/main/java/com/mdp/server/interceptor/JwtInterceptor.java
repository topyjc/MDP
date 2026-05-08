package com.mdp.server.interceptor;

import com.mdp.server.util.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (request.getMethod().equals("OPTIONS")) {
            return true;
        }

        // 헤더에서 토큰 꺼내기
        String token = request.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.substring(7); // "Bearer " 이후의 실제 토큰 문자열만 추출

            // 토큰 유효 검사
            if (jwtUtil.validateToken(jwt)) {
                // 유효하다면 토큰에서 userId를 뽑아내서 Request 객체에 담아둠
                request.setAttribute("userId", jwtUtil.extractUserId(jwt));
                return true;
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        response.setContentType("text/plain; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("인증에 실패했습니다. 유효한 토큰이 필요합니다.");

        return false;
        
    }
}