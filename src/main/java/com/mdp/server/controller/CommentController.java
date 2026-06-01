package com.mdp.server.controller;

import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DataService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/private/comments")
public class CommentController {

    private final DataService dataService;

    public CommentController(DataService dataService) {
        this.dataService = dataService;
    }

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
            dataDto.setData(commentData); // postId, content, likes, userId 포함

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