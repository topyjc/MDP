package com.mdp.server.controller;

import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DataService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/protectors")
public class ProtectorController {

    private final DataService dataService;

    public ProtectorController(DataService dataService) {
        this.dataService = dataService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerProtector(@RequestBody Map<String, Object> protectorData, HttpServletRequest request) {
        try {

            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인 정보가 없습니다."));
            }

            protectorData.put("userId", userId);

            DataDto dataDto = new DataDto();
            dataDto.setContent("plt");
            dataDto.setTable_num("2");
            dataDto.setTimestamp(System.currentTimeMillis());
            dataDto.setData(protectorData);

            boolean isSuccess = dataService.processData(dataDto);

            if (isSuccess) {
                return ResponseEntity.ok(Map.of(
                        "message", "보호자 등록이 완료되었습니다.",
                        "success", true
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "DB 서버 저장에 실패했습니다.",
                        "success", false
                ));
            }

        } catch (Exception e) {
            System.out.println("메인 서버 보호자 등록 에러 " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "서버 내부 오류가 발생했습니다."));
        }
    }
}