package com.mdp.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.dto.SensorMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SensorWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        System.out.println("[WS] connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        System.out.println("[WS] received from client: " + message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        System.out.println("[WS] disconnected: " + session.getId());
    }

    public void broadcast(SensorMessage messageDto) {
        // 🔍 1. 현재 연결된 클라이언트가 몇 명인지 확인!
        System.out.println("[WS 디버그] 현재 연결된 세션 수: " + sessions.size());

        try {
            String json = objectMapper.writeValueAsString(messageDto);
            // 🔍 2. JSON으로 변환된 메시지가 잘 만들어졌는지 확인!
            System.out.println("[WS 디버그] 변환된 JSON: " + json);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                    // 🔍 3. 실제 발송 성공 여부 확인!
                    System.out.println("[WS 디버그] 발송 완료 대상: " + session.getId());
                }
            }
        } catch (Exception e) {
            System.out.println("[WS] broadcast failed");
            e.printStackTrace();
        }
    }
}