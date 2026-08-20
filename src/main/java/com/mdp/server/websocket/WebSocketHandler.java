package com.mdp.server.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

    // 💡 전체 브로드캐스트용 세션 목록 (기존 유지)
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    // 💡 특정 유저 1:1 알림용 (userId -> 안전한 Session) 매핑
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketSession safeSession =
                new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES);
        sessions.add(safeSession);
        System.out.println("[WS] connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        System.out.println("[WS] received from client: " + message.getPayload());

        try {
            // 프론트엔드가 보낸 JSON 메시지 파싱
            JsonNode node = objectMapper.readTree(message.getPayload());

            // 💡 앱에서 로그인을 성공하고 {"type": "REGISTER", "userId": "아이디"} 를 보냈을 때
            if (node.has("type") && "REGISTER".equals(node.get("type").asText()) && node.has("userId")) {
                String userId = node.get("userId").asText();

                // 원본 세션이 아닌, afterConnectionEstablished에서 포장해둔 안전한 세션을 찾아 매핑합니다.
                WebSocketSession safeSession = sessions.stream()
                        .filter(s -> s.getId().equals(session.getId()))
                        .findFirst()
                        .orElse(session);

                userSessions.put(userId, safeSession);
                System.out.println("[WS] 유저 등록 완료: " + userId + " (세션: " + session.getId() + ")");
            }
        } catch (Exception e) {
            // JSON이 아닌 일반 텍스트가 오거나 파싱에 실패해도 서버가 죽지 않도록 무시
            System.out.println("[WS] JSON 파싱 불가 또는 일반 메시지: " + message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 브로드캐스트 셋에서 제거
        sessions.removeIf(s -> s.getId().equals(session.getId()));

        // 💡 1:1 매핑 맵에서도 제거 (로그아웃 / 앱 종료 시)
        userSessions.values().removeIf(s -> s.getId().equals(session.getId()));

        System.out.println("[WS] disconnected: " + session.getId());
    }

    // =========================================================
    // 💡 추가된 기능: 특정 유저에게만 메시지 전송 (1:1 타겟팅)
    // =========================================================
    public void sendToSpecificUser(String userId, Object messageDto) {
        WebSocketSession safeSession = userSessions.get(userId);

        if (safeSession != null) {
            try {
                if (safeSession.isOpen()) {
                    String json = objectMapper.writeValueAsString(messageDto);
                    safeSession.sendMessage(new TextMessage(json));
                    System.out.println("[WS] " + userId + "에게 1:1 알림 전송 완료");
                } else {
                    // 세션이 닫혀있으면 쓰레기 데이터 정리
                    userSessions.remove(userId);
                    sessions.remove(safeSession);
                }
            } catch (IOException e) {
                System.out.println("[WS] " + userId + " 세션 전송 실패, 맵에서 제거");
                userSessions.remove(userId);
                sessions.remove(safeSession);
            }
        } else {
            System.out.println("[WS] 수신자(" + userId + ")가 현재 미접속 상태입니다.");
        }
    }

    // =========================================================
    // 기존 기능: 모든 유저에게 메시지 전송 (브로드캐스트)
    // =========================================================
    public void broadcast(Object messageDto) {
        try {
            String json = objectMapper.writeValueAsString(messageDto);
            TextMessage textMessage = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                    } else {
                        sessions.remove(session);
                    }
                } catch (IOException e) {
                    System.out.println("[WS] session send failed, removing: " + session.getId());
                    sessions.remove(session);
                }
            }
        } catch (IOException e) {
            System.out.println("[WS] broadcast failed (serialize)");
            e.printStackTrace();
        }
    }
}