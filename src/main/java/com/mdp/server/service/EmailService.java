package com.mdp.server.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    // 발신자 표시 이름 - "MDP 알림" 대신 서비스 이름에 맞게 바꿔서 쓰세요.
    private static final String FROM_NAME = "MDP 알림";

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean send(String toEmail, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(fromAddress, FROM_NAME, "UTF-8"));
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(buildHtml(content), true); // true = HTML

            mailSender.send(message);
            System.out.println("[EMAIL] 발송 성공: " + toEmail);
            return true;

        } catch (Exception e) {
            System.out.println("[EMAIL] 발송 실패: " + e.getMessage());
            return false;
        }
    }

    private String buildHtml(String content) {
        return """
                <div style="font-family: 'Malgun Gothic', sans-serif; font-size: 14px; color: #333;">
                    <p>%s</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
                    <p style="font-size: 12px; color: #999;">
                        본 메일은 MDP 서비스에서 발송되었습니다.<br>
                        문의: %s
                    </p>
                </div>
                """.formatted(content.replace("\n", "<br>"), fromAddress);
    }
}