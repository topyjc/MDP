package com.mdp.server.controller;

import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/data") // 기본 URL 주소 설정
@CrossOrigin(origins = "*")  // 프론트엔드(웹)에서 API 호출 시 발생하는 CORS 에러 방지
public class DataController {

    private final DataService dataService;

    public DataController(DataService dataService) {
        this.dataService = dataService;
    }

    /**
     * [데이터 조회 API]
     * 웹/앱에서 특정 작품의 특정 타입 데이터를 요청할 때 사용합니다.
     * * @param content 작품명 (예: 스마트홈, 도로, 가로등)
     * @param tableNum 데이터 타입 (예: 1=실시간 센서값, 2=스위치 제어값)
     * * 호출 예시: GET http://메인서버IP:8080/api/data/스마트홈/1
     */
    @GetMapping("/{content}/{tableNum}")
    public ResponseEntity<DataDto> getTargetData(
            @PathVariable String content,
            @PathVariable String tableNum) {

        System.out.println("[API 요청 수신] 작품명: " + content + ", 타입: " + tableNum);

        // 1. 서비스 계층에 데이터 조회를 지시
        DataDto result = dataService.fetchData(content, tableNum);

        // 2. DB에서 가져온 데이터가 없으면 204 (No Content) 상태 코드 반환
        if (result == null) { // 비어있는지 확인할 때 isEmpty() 대신 null 체크!
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }
}