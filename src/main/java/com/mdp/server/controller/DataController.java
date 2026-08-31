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
    // DataController.java
    @GetMapping("/{content}/{tableNum}")
    public ResponseEntity<DataDto> getTargetData(
            @PathVariable String content,
            @PathVariable String tableNum) {

        DataDto result;

        // 💡 신호등(road/3) 및 가로등(streetlight/0) 데이터를 배열(List) 전체 조회 처리
        if (("road".equals(content) && "3".equals(tableNum)) ||
                ("streetlight".equals(content) && "0".equals(tableNum))) {
            result = dbServerClient.fetchAllData(content, tableNum);
        } else {
            result = dataService.fetchData(content, tableNum);
        }

        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }
}