package com.mdp.server.controller;

import com.mdp.server.dto.DataDto;
import com.mdp.server.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * DB 서버에서 발생한 알림(위험 감지 등)을 받아 등록된 이메일로 발송하는 컨트롤러.
 * InterceptorConfig에서 "/api/alert/**" 는 이미 JWT 인증 제외 대상이라
 * DB 서버가 별도 토큰 없이 서버 간 호출로 바로 쓸 수 있음.
 */
@RestController
@RequestMapping("/api/alert")
public class EmailAlertController {

    // 아주 기초적인 이메일 형식 검증용 정규식
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private final EmailService emailService;

    public EmailAlertController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/mail")
    public ResponseEntity<?> sendAlertMail(@RequestBody DataDto request) {
        // 요청이 실제로 이 메서드까지 도달했는지 항상 확인할 수 있도록 무조건 찍는 로그
        System.out.println("[MAIL] /api/alert/mail 요청 수신: content=" + request.getContent()
                + ", table_num=" + request.getTable_num());

        Map<String, Object> data = request.getData();

        if (data == null) {
            System.out.println("[MAIL][WARN] data 필드가 null 입니다. (요청 본문 구조 확인 필요)");
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "data 필드가 비어 있습니다."
            ));
        }

        String mail = asString(data.get("mail"));
        String message = asString(data.get("message"));
        String alertStatus = asString(data.get("alert_status"));
        String userId = asString(data.get("userId"));

        if (mail == null || mail.isBlank()) {
            System.out.println("[MAIL][WARN] mail 필드가 없어 메일 발송을 건너뜁니다. (userId=" + userId + ")");
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "수신자 이메일(mail) 정보가 없습니다."
            ));
        }

        if (!EMAIL_PATTERN.matcher(mail).matches()) {
            System.out.println("[MAIL][WARN] 유효하지 않은 이메일 형식: " + mail + " (userId=" + userId + ")");
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "이메일 형식이 올바르지 않습니다."
            ));
        }

        String subject = buildSubject(request.getContent(), alertStatus);
        String body = (message == null || message.isBlank())
                ? "긴급 상황이 감지되었습니다. 확인해주세요."
                : message;

        boolean isSuccess = emailService.send(mail, subject, body);

        if (isSuccess) {
            System.out.println("[MAIL] 알림 메일 발송 완료 -> " + mail + " (userId=" + userId + ")");
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "메일 발송 완료"
            ));
        } else {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "메일 발송 실패"
            ));
        }
    }

    private String buildSubject(String content, String alertStatus) {
        String prefix = "DANGER".equalsIgnoreCase(alertStatus) ? "[긴급 알림]" : "[알림]";
        String category = switch (content == null ? "" : content) {
            case "road" -> "도로 위험 감지";
            case "house" -> "스마트홈 이상 감지";
            default -> "상황 알림";
        };
        return prefix + " " + category;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}