package com.mdp.server.controller;

import com.mdp.server.client.DbServerClient;
import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DataService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/private/comments")
public class CommentController {

    private final DataService dataService;
    private final DbServerClient dbServerClient;

    public CommentController(DataService dataService, DbServerClient dbServerClient) {
        this.dataService = dataService;
        this.dbServerClient = dbServerClient;
    }

    // 💡 [새로 추가된 댓글 조회 GET 핸들러]
    @GetMapping
    public ResponseEntity<?> getComments(
            @RequestParam(name = "postId", required = false) String postId,
            HttpServletRequest request) {
        try {
            // 1. 로그인 유저 검증
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                System.out.println("[FAIL] 댓글 조회 실패: 인증 정보(userId) 없음");
                return ResponseEntity.status(401).body(Map.of("message", "로그인 정보가 없습니다.", "success", false));
            }

            // 2. postId 파라미터 검증
            if (postId == null || postId.isBlank()) {
                System.out.println("[FAIL] 댓글 조회 실패: postId 누락됨");
                return ResponseEntity.badRequest().body(Map.of("message", "postId가 누락되었습니다.", "success", false));
            }

            // 3. DB 서버에서 댓글 전체 목록 조회 (content: "plt", table_num: "2")
            DataDto responseDto = dbServerClient.fetchAllData("plt", "2");

            // 4. 무조건 배열([]) 형태로 보장하기 위한 리스트 생성
            List<Object> commentList = new ArrayList<>();

            if (responseDto != null && responseDto.getData() != null) {
                Object rawData = responseDto.getData();

                // Case A: DB 서버 응답이 배열(List)인 경우
                if (rawData instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> map) {
                            String itemPostId = String.valueOf(map.get("postId"));
                            if (postId.equals(itemPostId)) {
                                commentList.add(item);
                            }
                        }
                    }
                }
                // Case B: DB 서버 응답이 객체(Map) 형태인 경우
                else if (rawData instanceof Map<?, ?> map) {
                    // Map 내부 { "list": [...] } 구조 처리
                    if (map.containsKey("list") && map.get("list") instanceof List<?> innerList) {
                        for (Object item : innerList) {
                            if (item instanceof Map<?, ?> innerMap) {
                                String itemPostId = String.valueOf(innerMap.get("postId"));
                                if (postId.equals(itemPostId)) {
                                    commentList.add(item);
                                }
                            }
                        }
                    } else {
                        // 단일 댓글 객체 1개만 온 경우 -> postId 확인 후 배열에 1건 추가
                        String itemPostId = String.valueOf(map.get("postId"));
                        if (postId.equals(itemPostId)) {
                            commentList.add(map);
                        }
                    }
                }
            }

            System.out.println("[SUCCESS] 댓글 조회 성공! 요청 postId: " + postId + " | 반환된 댓글 수: " + commentList.size());

            // 5. 프론트엔드 요구사항에 맞춘 최종 응답 반환 (무조건 data: [])
            return ResponseEntity.ok(Map.of(
                    "message", "댓글 조회 성공",
                    "data", commentList
            ));

        } catch (Exception e) {
            System.out.println("[ERROR] 댓글 조회 예외 발생 : " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "서버 오류 발생"));
        }
    }

    // 댓글 작성 (기존 로직 유지)
    @PostMapping
    public ResponseEntity<?> createComment(@RequestBody Map<String, Object> commentData, HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인 정보가 없습니다."));
            }

            commentData.put("userId", userId);
            commentData.putIfAbsent("likes", 0);

            DataDto dataDto = new DataDto();
            dataDto.setContent("plt");
            dataDto.setTable_num("2");
            dataDto.setTimestamp(System.currentTimeMillis());
            dataDto.setData(commentData);

            boolean isSuccess = dataService.processData(dataDto);

            if (isSuccess) {
                return ResponseEntity.ok(Map.of(
                        "message", "댓글이 성공적으로 등록되었습니다.",
                        "success", true
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "댓글 등록에 실패했습니다.",
                        "success", false
                ));
            }

        } catch (Exception e) {
            System.out.println("[댓글 등록 에러] " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "서버 오류 발생"));
        }
    }
}