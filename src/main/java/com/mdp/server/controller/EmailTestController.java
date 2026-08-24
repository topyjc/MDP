package com.mdp.server.controller;

import com.mdp.server.service.EmailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmailTestController {

    private final EmailService emailService;

    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/test/email")
    public String testEmail(@RequestParam String to) {
        boolean result = emailService.send(to, "테스트 메일", "이메일 발송 테스트입니다.");
        return result ? "발송 성공" : "발송 실패";
    }
}