package com.mdp.server.controller;

import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DataService;
import com.mdp.server.util.JwtUtil; // 🔥 추가됨
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final DataService dataService;
    private final JwtUtil jwtUtil; // 🔥 토큰 발급기 추가

    // 생성자로 주입받기
    public UserController(DataService dataService, JwtUtil jwtUtil) {
        this.dataService = dataService;
        this.jwtUtil = jwtUtil;
    }

    // ... (signUp 메서드 생략) ...

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> loginData) {
        try {
            loginData.putIfAbsent("isAdmin", 0);

            DataDto requestDto = new DataDto();
            requestDto.setContent("plt");
            requestDto.setTable_num("4");
            requestDto.setData(loginData);

            // dataService.processData(requestDto); (DB 서버 전송 로직)

            // --- DB 서버 확인 완료 가정 ---
            boolean isSuccess = true;
            String userId = (String) loginData.get("userId");
            int isAdmin = (int) loginData.get("isAdmin");

            if (isSuccess) {
                // 🔥 드디어 진짜 JWT 토큰 발급!
                String token = jwtUtil.generateToken(userId, isAdmin);

                return ResponseEntity.ok(Map.of(
                        "message", "로그인 성공!",
                        "userId", userId,
                        "isAdmin", isAdmin,
                        "token", token // ⬅️ 발급된 진짜 토큰을 클라이언트로 내려줍니다.
                ));
            } else {
                return ResponseEntity.status(401).body(Map.of("message", "아이디 또는 비밀번호가 일치하지 않습니다."));
            }

        } catch (Exception e) {
            System.out.println("[로그인 통신 실패] " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "서버 통신 오류가 발생했습니다."));
        }
    }
}