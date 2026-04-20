package com.mdp.server.controller;

import com.mdp.server.dto.DataDto;
import com.mdp.server.client.DbServerClient;
import com.mdp.server.service.DataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final DbServerClient dbServerClient;

    public UserController(DbServerClient dbServerClient) {
        this.dbServerClient = dbServerClient;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody Map<String, Object> requestData) {
        try {
            // 1. 앱에서 isAdmin을 안 보냈을 경우에만 기본값 0(일반유저)으로 세팅
            // 웹에서 "isAdmin": 1 을 보냈다면 그 값이 그대로 유지됩니다!
            requestData.putIfAbsent("isAdmin", 0);

            // 2. 기존 DataDto 껍데기 생성 및 세팅
            DataDto dataDto = new DataDto();
            dataDto.setContent("plt");
            dataDto.setTable_num("0");
            dataDto.setData(requestData); // 클라이언트가 보낸 데이터 그대로 삽입

            // 3. DB 서버로 쿨하게 전송!
            dbServerClient.sendData(dataDto);

            return ResponseEntity.ok("회원가입 정보가 DB 서버로 성공적으로 전송되었습니다.");

        } catch (Exception e) {
            System.out.println("[DB전송 실패] " + e.getMessage());
            return ResponseEntity.internalServerError().body("회원가입 처리 중 오류가 발생했습니다.");
        }
    }

    // ... 기존 signUp 메서드 아래에 추가 ...

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> loginData) {
        try {
            // DB 서버에서 로그인 시에도 isAdmin 필드를 요구한다면 기본값 0을 넣어줍니다.
            // (만약 DB가 로그인 시에는 userId와 password만 본다면 이 줄은 빼셔도 됩니다!)
            loginData.putIfAbsent("isAdmin", 0);

            // 1. 약속된 규약대로 DataDto 껍데기 세팅
            DataDto requestDto = new DataDto();
            requestDto.setContent("plt"); // content는 plt로 고정
            requestDto.setTable_num("4"); // 로그인은 테이블 번호 4번!
            requestDto.setData(loginData); // 클라이언트가 보낸 데이터 삽입

            // 2. DB 서버로 전송 (기존 DataService 재활용)
            // String dbResponse = dataService.processData(requestDto);

            // --- 아래는 DB 서버의 응답을 확인하는 로직입니다 ---
            // (임시) DB 서버가 성공적으로 확인해줬다고 가정
            boolean isSuccess = true;
            int isAdmin = (int) loginData.get("isAdmin");

            if (isSuccess) {
                // 3. 로그인 성공 시 클라이언트에게 응답
                return ResponseEntity.ok(Map.of(
                        "message", "로그인 성공!",
                        "userId", loginData.get("userId"),
                        "isAdmin", isAdmin,
                        "token", "임시_방문증_토큰_12345" // 곧 JWT로 바꿀 부분
                ));
            } else {
                // 4. 로그인 실패 시
                return ResponseEntity.status(401).body(Map.of("message", "아이디 또는 비밀번호가 일치하지 않습니다."));
            }

        } catch (Exception e) {
            System.out.println("[로그인 통신 실패] " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "서버 통신 오류가 발생했습니다."));
        }
    }
}