package com.mdp.server.controller;

import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DataService;
import com.mdp.server.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final DataService dataService;
    private final JwtUtil jwtUtil;

    // 생성자로 주입받기
    public UserController(DataService dataService, JwtUtil jwtUtil) {
        this.dataService = dataService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * [회원가입 API]
     * 복구 완료! DB 서버로 회원가입 정보를 전송합니다.
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody Map<String, Object> signUpData) {
        try {
            // 기본 권한이 안 넘어왔을 경우 일반 유저(0)로 세팅
            signUpData.putIfAbsent("isAdmin", 0);

            DataDto requestDto = new DataDto();
            requestDto.setContent("plt");
            requestDto.setTable_num("0"); // 🔥 DB 서버와 약속한 회원가입 테이블 번호로 맞춰주세요! (예: 3)
            requestDto.setData(signUpData);

            // DB 서버로 회원가입 요청 전송
            boolean isSuccess = dataService.processData(requestDto);

            if (isSuccess) {
                return ResponseEntity.ok(Map.of(
                        "message", "회원가입이 성공적으로 완료되었습니다.",
                        "success", true
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "회원가입에 실패했습니다. (아이디 중복 등)",
                        "success", false
                ));
            }

        } catch (Exception e) {
            System.out.println("[회원가입 통신 실패] " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "서버 통신 오류가 발생했습니다."
            ));
        }
    }

    /**
     * [로그인 API]
     * DB 검증 후 성공 시 JWT 토큰을 발급합니다.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> loginData) {
        try {
            loginData.putIfAbsent("isAdmin", 0);

            DataDto requestDto = new DataDto();
            requestDto.setContent("plt");
            requestDto.setTable_num("4"); // 🔥 로그인 테이블 번호
            requestDto.setData(loginData);

            // DB 서버 확인 로직
            boolean isSuccess = dataService.processData(requestDto);
            String userId = (String) loginData.get("userId");

            // Map에서 가져온 값이 Integer일 수도 있으므로 안전하게 파싱 (에러 방지용)
            int isAdmin = 0;
            if (loginData.get("isAdmin") != null) {
                isAdmin = Integer.parseInt(String.valueOf(loginData.get("isAdmin")));
            }

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
                return ResponseEntity.status(401).body(Map.of(
                        "message", "아이디 또는 비밀번호가 일치하지 않습니다."
                ));
            }

        } catch (Exception e) {
            System.out.println("[로그인 통신 실패] " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "서버 통신 오류가 발생했습니다."
            ));
        }
    }
}