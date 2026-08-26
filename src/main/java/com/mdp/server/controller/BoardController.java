package com.mdp.server.controller;

import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DataService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mdp.server.client.DbServerClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BoardController {

    private final DataService dataService;
    private final DbServerClient dbServerClient;

    public BoardController(DataService dataService, DbServerClient dbServerClient) {
        this.dataService = dataService;
        this.dbServerClient = dbServerClient;
    }

    @GetMapping("/public/boards")
    public ResponseEntity<?> getBoards() {
        try {
            DataDto responseDto = dbServerClient.fetchAllData("plt", "1");

            Object rawData = responseDto.getData();
            Object finalData = rawData;

            // 💡 DB 서버가 { list: [...] } 형태로 넘겨줄 경우 내부 배열만 추출
            if (rawData instanceof Map<?, ?> map && map.get("list") instanceof List<?> list) {
                finalData = list;
            }

            int itemCount = (finalData instanceof List<?> list) ? list.size() : 0;

            System.out.println("[SUCCESS] 게시판 전체 목록 조회 성공! 데이터 개수: " + itemCount);
            System.out.println("[SUCCESS] 수신된 전체 목록: " + finalData);

            return ResponseEntity.ok(Map.of(
                    "message", "게시판 목록 조회 성공",
                    "data", finalData // 프론트엔드에는 순수 [ {...}, {...} ] 배열 전달
            ));
        } catch (Exception e) {
            System.out.println("[ERROR] 게시판 전체 조회 실패 : " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "조회 오류"));
        }
    }

    @PostMapping("/private/boards")
    public ResponseEntity<?> createBoard(@RequestBody Map<String, Object> boardData, HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                System.out.println("[FAIL] 게시글 작성 실패: 인증 정보(userId) 없음");
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
                // 💡 [디버깅 Log] 게시글 작성 성공
                System.out.println("[SUCCESS] 게시글 작성 성공! 작성자: " + userId + " | Payload: " + boardData);
                return ResponseEntity.ok(Map.of("message", "게시글이 성공적으로 작성되었습니다.", "success", true));
            } else {
                System.out.println("[FAIL] 게시글 작성 DB 반환 실패 (isSuccess=false) | 작성자: " + userId);
                return ResponseEntity.badRequest().body(Map.of("message", "게시글 작성에 실패했습니다.", "success", false));
            }

        } catch (Exception e) {
            System.out.println("[ERROR] 게시글 작성 예외 발생 : " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "서버 오류"));
        }
    }

    @PostMapping("/private/boards/like")
    public ResponseEntity<?> handleBoardLike(
            @RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                System.out.println("[FAIL] 좋아요 실패: 인증 정보(userId) 없음");
                return ResponseEntity.status(401).body(Map.of("message", "로그인 정보가 없습니다.", "success", false));
            }

            Object rawPostId = requestData.get("postId");
            if (rawPostId == null) {
                System.out.println("[FAIL] 좋아요 실패: postId 누락됨");
                return ResponseEntity.badRequest().body(Map.of("message", "게시글 ID(postId)가 누락되었습니다.", "success", false));
            }

            int postId = 0;
            if (rawPostId instanceof Number number) {
                postId = number.intValue();
            } else if (rawPostId instanceof String str) {
                postId = Integer.parseInt(str);
            }

            DataDto requestDto = new DataDto();
            requestDto.setContent("plt");
            requestDto.setTable_num("5");
            requestDto.setTimestamp(System.currentTimeMillis());

            Map<String, Object> innerData = new HashMap<>();
            innerData.put("postId", postId);

            requestDto.setData(innerData);

            boolean isSuccess = dataService.processData(requestDto);

            if (isSuccess) {
                // 💡 [디버깅 Log] 좋아요 반영 성공
                System.out.println("[SUCCESS] 좋아요 반영 성공! 유저: " + userId + " | 게시글 ID: " + postId);
                return ResponseEntity.ok(Map.of(
                        "message", "좋아요가 성공적으로 반영되었습니다.",
                        "success", true
                ));
            } else {
                System.out.println("[FAIL] 좋아요 DB 반영 실패 (isSuccess=false) | 유저: " + userId + " | 게시글 ID: " + postId);
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "좋아요 반영에 실패했습니다.",
                        "success", false
                ));
            }

        } catch (Exception e) {
            System.out.println("[ERROR] 좋아요 처리 예외 발생 : " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "서버 오류"));
        }
    }

    @PostMapping("/private/boards/update")
    public ResponseEntity<?> updateBoard(
            @RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        try {
            String loginUserId = (String) request.getAttribute("userId");
            if (loginUserId == null) {
                System.out.println("[FAIL] 게시글 수정 실패: 인증 정보 없음");
                return ResponseEntity.status(401).body(Map.of(
                        "message", "로그인 정보가 없습니다.",
                        "success", false
                ));
            }

            Object rawPostId = requestData.get("postId");
            String title = (String) requestData.get("title");
            String content = (String) requestData.get("content");

            if (rawPostId == null || title == null || content == null) {
                System.out.println("[FAIL] 게시글 수정 실패: 필수 항목 누락 (postId=" + rawPostId + ", title=" + title + ")");
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

            Map<String, Object> innerData = new HashMap<>();
            innerData.put("userId", loginUserId);
            innerData.put("postId", postId);
            innerData.put("title", title);
            innerData.put("content", content);

            requestDto.setData(innerData);

            boolean isSuccess = dataService.processData(requestDto);

            if (isSuccess) {
                // 💡 [디버깅 Log] 게시글 수정 성공
                System.out.println("[SUCCESS] 게시글 수정 성공! 작성자: " + loginUserId + " | 게시글 ID: " + postId + " | 수정 제목: " + title);
                return ResponseEntity.ok(Map.of(
                        "message", "게시글이 성공적으로 수정되었습니다.",
                        "success", true
                ));
            } else {
                System.out.println("[FAIL] 게시글 수정 DB 반영 실패 | 작성자: " + loginUserId + " | 게시글 ID: " + postId);
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "게시글 수정에 실패했습니다.",
                        "success", false
                ));
            }

        } catch (Exception e) {
            System.out.println("[ERROR] 게시글 수정 예외 발생 : " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "서버 오류가 발생했습니다."
            ));
        }
    }

    @PostMapping("/private/boards/delete")
    public ResponseEntity<?> deleteBoard(
            @RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        try {
            String loginUserId = (String) request.getAttribute("userId");
            if (loginUserId == null) {
                System.out.println("[FAIL] 게시글 삭제 실패: 인증 정보 없음");
                return ResponseEntity.status(401).body(Map.of(
                        "message", "로그인 정보가 없습니다.",
                        "success", false
                ));
            }

            Object rawPostId = requestData.get("postId");

            if (rawPostId == null) {
                System.out.println("[FAIL] 게시글 삭제 실패: postId 누락됨");
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
                // 💡 [디버깅 Log] 게시글 삭제 성공
                System.out.println("[SUCCESS] 게시글 삭제 성공! 요청 유저: " + loginUserId + " | 삭제된 게시글 ID: " + postId);
                return ResponseEntity.ok(Map.of(
                        "message", "게시글이 성공적으로 삭제되었습니다.",
                        "success", true
                ));
            } else {
                System.out.println("[FAIL] 게시글 삭제 DB 반영 실패 | 요청 유저: " + loginUserId + " | 게시글 ID: " + postId);
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "게시글 삭제에 실패했습니다.",
                        "success", false
                ));
            }

        } catch (Exception e) {
            System.out.println("[ERROR] 게시글 삭제 예외 발생 : " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "서버 오류가 발생했습니다."
            ));
        }
    }
}