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

    /**
     * 🔓 [조회] 게시판 목록 보기 (JWT 필요 없음 - Public)
     */
    @GetMapping("/public/boards")
    public ResponseEntity<?> getBoards() {
        try {
            // DB 서버에 게시판 데이터(목록) 요청
            // (예: 게시판 테이블 번호가 "10"이라고 가정)
            DataDto responseDto = dataService.fetchData("plt", "1");

            return ResponseEntity.ok(Map.of(
                    "message", "게시판 목록 조회 성공",
                    "data", responseDto.getData() // DB 서버가 보내준 게시글 리스트
            ));
        } catch (Exception e) {
            System.out.println("[게시판 조회 실패] " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "조회 오류"));
        }
    }

    /**
     * 🚨 [작성] 새 게시글 쓰기 (JWT 필수 - Private)
     */
    @PostMapping("/private/boards")
    public ResponseEntity<?> createBoard(@RequestBody Map<String, Object> boardData, HttpServletRequest request) {
        try {
            // 1. JWT 인터셉터가 request에 담아준 userId를 꺼냅니다.
            // (누가 썼는지 DB에 저장해야 하니까 필수!)
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인 정보가 없습니다."));
            }

            // 2. 작성자 아이디를 DB로 보낼 데이터에 몰래 끼워 넣습니다.
            boardData.put("userid", userId);

            // 3. DB 서버로 보낼 DTO 포장
            DataDto requestDto = new DataDto();
            requestDto.setContent("plt");
            requestDto.setTable_num("1"); // 게시판 쓰기 테이블 번호
            requestDto.setTimestamp(System.currentTimeMillis());
            requestDto.setData(boardData);

            // 4. DB 서버로 전송!
            boolean isSuccess = dataService.processData(requestDto);

            if (isSuccess) {
                return ResponseEntity.ok(Map.of("message", "게시글이 성공적으로 작성되었습니다.", "success", true));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "게시글 작성에 실패했습니다.", "success", false));
            }

        } catch (Exception e) {
            System.out.println("[게시글 작성 실패] " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "서버 오류"));
        }
    }
}