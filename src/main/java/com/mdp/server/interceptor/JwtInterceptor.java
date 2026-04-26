package com.mdp.server.interceptor;

import com.mdp.server.util.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// 🔥 javax.servlet 대신 jakarta.servlet으로 변경!
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
        // 브라우저의 사전 요청(Preflight, OPTIONS)은 토큰 검사 없이 바로 통과
        if (request.getMethod().equals("OPTIONS")) {
            return true;
        }

        // 헤더에서 토큰 꺼내기
        String token = request.getHeader("Authorization");

        // "Bearer "로 시작하는 토큰이 있는지 확인
        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.substring(7); // "Bearer " 이후의 실제 토큰 문자열만 추출

            // 토큰이 유효한지 검사
            if (jwtUtil.validateToken(jwt)) {
                // 유효하다면 토큰에서 userId를 뽑아내서 Request 객체에 담아둠
                // (나중에 컨트롤러에서 request.getAttribute("userId")로 꺼내 쓸 수 있음)
                request.setAttribute("userId", jwtUtil.extractUserId(jwt));
                return true;
            }
        }

        // 토큰이 없거나 유효하지 않으면 401(Unauthorized) 에러 반환
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("인증에 실패했습니다. 유효한 토큰이 필요합니다.");

        return false; // 컨트롤러로 요청을 넘기지 않고 여기서 차단
    }
}