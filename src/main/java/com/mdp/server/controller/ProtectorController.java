package com.mdp.server.controller;

import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DataService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/protectors")
public class ProtectorController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final DataService dataService;

    @Value("${db.server.url}")
    private String dbServerUrl;

    // DataService 생성자 주입
    public ProtectorController(DataService dataService) {
        this.dataService = dataService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerWard(@RequestBody Map<String, String> requestData, HttpServletRequest request) {
        try {
            // 1. JWT 인터셉터가 세팅해둔 보호자(본인) 아이디 추출
            String protectorId = (String) request.getAttribute("userId");
            if (protectorId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인 정보가 없습니다."));
            }

            // 2. 앱에서 보낸 피보호자 아이디 추출
            String wardId = requestData.get("wardId");
            if (wardId == null || wardId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "피보호자 ID를 입력해주세요."));
            }

            // 3. 방어 코드: 본인을 피보호자로 등록하려는 경우 차단
            if (protectorId.equals(wardId)) {
                return ResponseEntity.badRequest().body(Map.of("message", "본인을 피보호자로 등록할 수 없습니다."));
            }

            // 5. DB 서버 저장: DataService를 이용하여 전송
            DataDto dataDto = new DataDto();
            dataDto.setContent("plt");
            dataDto.setTable_num("4");
            dataDto.setTimestamp(System.currentTimeMillis());

            // data 필드 맵핑
            Map<String, Object> innerData = new HashMap<>();
            innerData.put("userId", protectorId);
            innerData.put("ward_user_id", wardId);
            dataDto.setData(innerData);

            boolean isSuccess = dataService.processData(dataDto);

            if (isSuccess) {
                System.out.println("[연동 성공] 보호자(" + protectorId + ") ➔ 피보호자(" + wardId + ")");
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "피보호자 등록이 성공적으로 완료되었습니다."
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "DB 서버 저장 실패"));
            }

        } catch (Exception e) {
            System.err.println("[ERROR] 피보호자 등록 중 에러 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "서버 간 통신 오류가 발생했습니다."));
        }
    }
}