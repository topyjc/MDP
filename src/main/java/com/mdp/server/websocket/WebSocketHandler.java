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
public class WebSocketHandler extends TextWebSocketHandler {

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

    // 기존: public void broadcast(SensorMessage messageDto) {

    // 변경: 어떤 객체든 다 받을 수 있도록 Object로 수정!
    public void broadcast(Object messageDto) {
        try {
            // ObjectMapper가 알아서 해당 객체의 형태에 맞게 JSON 문자열로 변환해 줍니다.
            String json = objectMapper.writeValueAsString(messageDto);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            System.out.println("[WS] broadcast failed");
            e.printStackTrace();
        }
    }
}