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

    @PostMapping("/private/boards/like")
    public ResponseEntity<?> handleBoardLike(
            @RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        try {
            // 1. 로그인 유저 확인 (보안 유지)
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인 정보가 없습니다.", "success", false));
            }

            // 2. 앱/웹에서 보낸 postId 추출
            Object rawPostId = requestData.get("postId");
            if (rawPostId == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "게시글 ID(postId)가 누락되었습니다.", "success", false));
            }

            int postId = 0;
            if (rawPostId instanceof Number number) {
                postId = number.intValue();
            } else if (rawPostId instanceof String str) {
                postId = Integer.parseInt(str);
            }

            // 3. DB 전송용 데이터 세팅 (table_num: 5, data: { postId })
            DataDto requestDto = new DataDto();
            requestDto.setContent("plt");
            requestDto.setTable_num("5");
            requestDto.setTimestamp(System.currentTimeMillis());

            Map<String, Object> innerData = new HashMap<>();
            innerData.put("postId", postId);

            requestDto.setData(innerData);

            // 4. DataService를 통해 DB 전송 (DB 서버가 받으면 +1 처리)
            boolean isSuccess = dataService.processData(requestDto);

            if (isSuccess) {
                return ResponseEntity.ok(Map.of(
                        "message", "좋아요가 성공적으로 반영되었습니다.",
                        "success", true
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "좋아요 반영에 실패했습니다.",
                        "success", false
                ));
            }

        } catch (Exception e) {
            System.out.println("좋아요 처리 실패 : " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "서버 오류"));
        }
    }


    // 게시글 수정
    @PostMapping("/private/boards/update")
    public ResponseEntity<?> updateBoard(
            @RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        try {
            // 1. 로그인 유저 검증 (토큰에서 내 아이디 추출)
            String loginUserId = (String) request.getAttribute("userId");
            if (loginUserId == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "message", "로그인 정보가 없습니다.",
                        "success", false
                ));
            }

            // 2. 앱/웹에서 보낸 필수 데이터 추출 (postId, title, content)
            Object rawPostId = requestData.get("postId");
            String title = (String) requestData.get("title");
            String content = (String) requestData.get("content");

            if (rawPostId == null || title == null || content == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "필수 데이터(postId, title, content)가 누락되었습니다.",
                        "success", false
                ));
            }

            String postId = String.valueOf(rawPostId);

            DataDto requestDto = new DataDto();
            requestDto.setContent("plt");
            requestDto.setTable_num("6");
            requestDto.setTimestamp(System.currentTimeMillis());

            // 내부 data 맵 조립
            Map<String, Object> innerData = new HashMap<>();
            innerData.put("userId", loginUserId);
            innerData.put("postId", postId);
            innerData.put("title", title);
            innerData.put("content", content);

            requestDto.setData(innerData);
            
            boolean isSuccess = dataService.processData(requestDto);

            if (isSuccess) {
                return ResponseEntity.ok(Map.of(
                        "message", "게시글이 성공적으로 수정되었습니다.",
                        "success", true
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "게시글 수정에 실패했습니다.",
                        "success", false
                ));
            }

        } catch (Exception e) {
            System.out.println("게시글 수정 실패 : " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "서버 오류가 발생했습니다."
            ));
        }
    }


    // 게시글 삭제

    @PostMapping("/private/boards/delete")
    public ResponseEntity<?> deleteBoard(
            @RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        try {
            // 1. 로그인 유저 검증 (토큰에서 아이디 추출)
            String loginUserId = (String) request.getAttribute("userId");
            if (loginUserId == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "message", "로그인 정보가 없습니다.",
                        "success", false
                ));
            }

            // 2. 웹에서 보낸 데이터 추출 (postId)
            Object rawPostId = requestData.get("postId");

            if (rawPostId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "필수 데이터(postId)가 누락되었습니다.",
                        "success", false
                ));
            }

            String postId = String.valueOf(rawPostId);

            DataDto requestDto = new DataDto();
            requestDto.setContent("plt");
            requestDto.setTable_num("7");
            requestDto.setTimestamp(System.currentTimeMillis());

            Map<String, Object> innerData = new HashMap<>();
            innerData.put("userId", loginUserId);
            innerData.put("postId", postId);

            requestDto.setData(innerData);
            
            boolean isSuccess = dataService.processData(requestDto);

            if (isSuccess) {
                return ResponseEntity.ok(Map.of(
                        "message", "게시글이 성공적으로 삭제되었습니다.",
                        "success", true
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "게시글 삭제에 실패했습니다.",
                        "success", false
                ));
            }

        } catch (Exception e) {
            System.out.println("게시글 삭제 실패 : " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "서버 오류가 발생했습니다."
            ));
        }
    }
}