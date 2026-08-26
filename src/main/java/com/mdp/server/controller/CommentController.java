package com.mdp.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.client.DbServerClient;
import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DataService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/private/comments")
public class CommentController {

    private final DataService dataService;
    private final DbServerClient dbServerClient;
    private final ObjectMapper objectMapper;

    public CommentController(DataService dataService, DbServerClient dbServerClient, ObjectMapper objectMapper) {
        this.dataService = dataService;
        this.dbServerClient = dbServerClient;
        this.objectMapper = objectMapper;
    }

    // 💡 GET /api/private/comments?postId=109 (댓글 목록 조회)
    @GetMapping
    public ResponseEntity<?> getComments(
            @RequestParam(name = "postId", required = false) String postId,
            HttpServletRequest request) {
        try {
            // 1. 인증 확인
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                System.out.println("[FAIL] 댓글 조회 실패: 인증 정보(userId) 없음");
                return ResponseEntity.status(401).body(Map.of("message", "로그인 정보가 없습니다.", "success", false));
            }

            // 2. postId 파라미터 검증 및 정수 변환
            if (postId == null || postId.isBlank()) {
                System.out.println("[FAIL] 댓글 조회 실패: postId 누락됨");
                return ResponseEntity.badRequest().body(Map.of("message", "postId가 누락되었습니다.", "success", false));
            }

            int numericPostId;
            try {
                numericPostId = Integer.parseInt(postId);
            } catch (NumberFormatException e) {
                System.out.println("[FAIL] 댓글 조회 실패: postId 형식이 숫자 형태가 않음 (" + postId + ")");
                return ResponseEntity.badRequest().body(Map.of("message", "올바른 postId 형식이 아닙니다.", "success", false));
            }

            // 3. DB 서버 전송용 DTO 생성 (table_num: "9")
            DataDto requestDto = new DataDto();
            requestDto.setContent("plt");
            requestDto.setTable_num("9");
            requestDto.setTimestamp(System.currentTimeMillis());

            Map<String, Object> innerData = new HashMap<>();
            innerData.put("postId", numericPostId);
            requestDto.setData(innerData);

            // 4. 💡 sendData(DataDto) 메서드 호출하여 JSON String 수신
            String jsonResponse = dbServerClient.sendData(requestDto);

            System.out.println(jsonResponse);

            List<?> commentList = new ArrayList<>();

            // 5. 수신한 JSON String 파싱 후 "comments" 리스트 추출
            if (jsonResponse != null && !jsonResponse.isBlank()) {
                Map<String, Object> responseMap = objectMapper.readValue(
                        jsonResponse,
                        new TypeReference<Map<String, Object>>() {}
                );

                if (responseMap.get("comments") instanceof List<?> list) {
                    commentList = list;
                }
            }

            System.out.println("[SUCCESS] 댓글 조회 성공! postId: " + numericPostId + " | 반환된 댓글 수: " + commentList.size());

            // 6. 프론트엔드로 배열 형태(data: [ ... ]) 응답
            return ResponseEntity.ok(Map.of(
                    "message", "댓글 조회 성공",
                    "data", commentList
            ));

        } catch (Exception e) {
            System.out.println("[ERROR] 댓글 조회 예외 발생 : " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "서버 오류 발생"));
        }
    }

    // 댓글 작성 (기존 유지)
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