package com.mdp.server.controller;

import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "*")
public class DataController {

    private final DataService dataService;

    public DataController(DataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping("/{content}/{tableNum}")
    public ResponseEntity<DataDto> getTargetData(
            @PathVariable String content,
            @PathVariable String tableNum) {

        System.out.println("[API request connect] group: " + content + ", type: " + tableNum);

        DataDto result = dataService.fetchData(content, tableNum);

        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }
}