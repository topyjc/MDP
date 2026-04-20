package com.mdp.server.controller;

import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final DataService dataService;

    public UserController(DataService dataService) {
        this.dataService = dataService;
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
            dataService.processData(dataDto);

            return ResponseEntity.ok("회원가입 정보가 DB 서버로 성공적으로 전송되었습니다.");

        } catch (Exception e) {
            System.out.println("[DB전송 실패] " + e.getMessage());
            return ResponseEntity.internalServerError().body("회원가입 처리 중 오류가 발생했습니다.");
        }
    }
}