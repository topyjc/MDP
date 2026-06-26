package com.mdp.server.controller;

import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DataService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

    @PostMapping("/private/boards/{id}/like")
    public ResponseEntity<?> handleBoardLike(
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        try {
            // 1. 로그인 유저 확인
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인 정보가 없습니다.", "success", false));
            }

            // 2. 좋아요 개수(likes) 안전하게 추출
            Object rawLikes = requestData.get("likes");
            int likesCount = 0;
            if (rawLikes instanceof Number number) {
                likesCount = number.intValue();
            } else if (rawLikes instanceof String str) {
                likesCount = Integer.parseInt(str);
            }

            // 3. DB 전송용 데이터 세팅
            DataDto requestDto = new DataDto();
            requestDto.setContent("plt");
            requestDto.setTable_num("1");
            requestDto.setTimestamp(System.currentTimeMillis());

            Map<String, Object> innerData = new HashMap<>();
            innerData.put("userId", userId);
            innerData.put("likes", likesCount);

            // TODO: 웹 개발자와 협의 후 postId 매핑 주석 해제
            // innerData.put("postId", id);

            requestDto.setData(innerData);

            // 4. DataService를 통해 DB 전송
            boolean isSuccess = dataService.processData(requestDto);

            if (isSuccess) {
                return ResponseEntity.ok(Map.of("message", "좋아요 정보가 성공적으로 반영되었습니다.", "success", true));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "좋아요 반영에 실패했습니다.", "success", false));
            }

        } catch (Exception e) {
            System.out.println("좋아요 처리 실패 : " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "서버 오류"));
        }
    }
}