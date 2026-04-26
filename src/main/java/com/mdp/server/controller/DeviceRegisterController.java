package com.mdp.server.controller;

import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DataService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/devices")
public class DeviceRegisterController {

    private final DataService dataService;

    public DeviceRegisterController(DataService dataService) {
        this.dataService = dataService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerDevice(@RequestBody Map<String, Object> qrData, HttpServletRequest request) {
        // 인터셉터가 담아준 userId 꺼내기
        String userId = (String) request.getAttribute("userId");

        // DB 서버 규격에 맞게 구성
        qrData.put("ownerId", userId); // 누구 소유인지 추가

        DataDto dataDto = new DataDto();
        dataDto.setContent("plt");
        dataDto.setTable_num("3"); // 기기 등록 테이블 3번(예시)
        dataDto.setData(qrData);

        dataService.processData(dataDto);
        return ResponseEntity.ok("기기가 " + userId + " 계정에 등록되었습니다.");
    }
}