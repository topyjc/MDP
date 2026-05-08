package com.mdp.server.controller;

import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DataService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class BoardController {

    private final DataService dataService;

    public BoardController(DataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping("/public/boards")
    public ResponseEntity<?> getBoards() {
        try {

            DataDto responseDto = dataService.fetchData("plt", "1");

            return ResponseEntity.ok(Map.of(
                    "message", "게시판 목록 조회 성공",
                    "data", responseDto.getData()
            ));
        } catch (Exception e) {
            System.out.println("게시판 조회 실패 : " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "조회 오류"));
        }
    }

    @PostMapping("/private/boards")
    public ResponseEntity<?> createBoard(@RequestBody Map<String, Object> boardData, HttpServletRequest request) {
        try {

            String userId = (String) request.getAttribute("userId");
            System.out.println("유저 아이디 " + userId);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인 정보가 없습니다."));
            }

            boardData.put("userId", userId);

            DataDto requestDto = new DataDto();
            requestDto.setContent("plt");
            requestDto.setTable_num("1");
            requestDto.setTimestamp(System.currentTimeMillis());
            requestDto.setData(boardData);

            boolean isSuccess = dataService.processData(requestDto);

            if (isSuccess) {
                return ResponseEntity.ok(Map.of("message", "게시글이 성공적으로 작성되었습니다.", "success", true));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "게시글 작성에 실패했습니다.", "success", false));
            }

        } catch (Exception e) {
            System.out.println("게시글 작성 실패 : " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "서버 오류"));
        }
    }
}