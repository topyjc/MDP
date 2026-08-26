package com.mdp.server.controller;

import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mdp.server.client.DbServerClient;

@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "*")
public class DataController {

    private final DataService dataService;
    private final DbServerClient dbServerClient;

    public DataController(DataService dataService, DbServerClient dbServerClient) {
        this.dataService = dataService;
        this.dbServerClient = dbServerClient;
    }

    @GetMapping("/{content}/{tableNum}")
    public ResponseEntity<DataDto> getTargetData(
            @PathVariable String content,
            @PathVariable String tableNum) {

        DataDto result;

        // 💡 도로팀(road) 신호등(3번) 데이터인 경우 배열 전체 조회(fetchAllData) 사용
        if ("road".equals(content) && "3".equals(tableNum)) {
            result = dbServerClient.fetchAllData(content, tableNum);
        } else {
            // 그 외 일반 센서 단건 조회
            result = dataService.fetchData(content, tableNum);
        }

        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }
}