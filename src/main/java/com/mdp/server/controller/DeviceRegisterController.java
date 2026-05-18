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

        String userId = (String) request.getAttribute("userId");

        qrData.put("userId", userId);

        DataDto dataDto = new DataDto();
        dataDto.setContent("house");
        dataDto.setTable_num("3");
        dataDto.setTimestamp(System.currentTimeMillis());
        dataDto.setData(qrData);

        dataService.processData(dataDto);
        return ResponseEntity.ok("기기가 " + userId + " 계정에 등록되었습니다.");
    }
}