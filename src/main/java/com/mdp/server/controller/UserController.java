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

    public UserController(DataService dataService, JwtUtil jwtUtil) {
        this.dataService = dataService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody Map<String, Object> signUpData) {
        try {
            signUpData.putIfAbsent("isAdmin", 0);

            DataDto requestDto = new DataDto();
            requestDto.setContent("plt");
            requestDto.setTable_num("0");
            requestDto.setTimestamp(System.currentTimeMillis());
            requestDto.setData(signUpData);

            boolean isSuccess = dataService.processData(requestDto);

            if (isSuccess) {
                return ResponseEntity.ok(Map.of(
                        "message", "회원가입이 성공적으로 완료되었습니다.",
                        "success", true
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "회원가입에 실패했습니다.",
                        "success", false
                ));
            }

        } catch (Exception e) {
            System.out.println("회원가입 통신 실패 : " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "서버 통신 오류가 발생했습니다."
            ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> loginData) {
        try {
            loginData.putIfAbsent("isAdmin", 0);

            DataDto requestDto = new DataDto();
            requestDto.setContent("plt");
            requestDto.setTable_num("4");
            requestDto.setTimestamp(System.currentTimeMillis());
            requestDto.setData(loginData);

            boolean isSuccess = dataService.processData(requestDto);
            String userId = (String) loginData.get("userId");

            int isAdmin = 0;
            if (loginData.get("isAdmin") != null) {
                isAdmin = Integer.parseInt(String.valueOf(loginData.get("isAdmin")));
            }

            if (isSuccess) {
                // JWT 토큰 발급
                String token = jwtUtil.generateToken(userId, isAdmin);

                return ResponseEntity.ok(Map.of(
                        "message", "로그인 성공!",
                        "userId", userId,
                        "isAdmin", isAdmin,
                        "token", token
                ));
            } else {
                return ResponseEntity.status(401).body(Map.of(
                        "message", "아이디 또는 비밀번호가 일치하지 않습니다."
                ));
            }

        } catch (Exception e) {
            System.out.println("로그인 통신 실패 : " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "서버 통신 오류가 발생했습니다."
            ));
        }
    }
}