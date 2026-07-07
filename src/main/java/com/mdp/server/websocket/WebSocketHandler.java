package com.mdp.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdp.server.dto.SensorMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    // 센서 이벤트(전용 스레드풀)와 미디어 이벤트(같은 전용 스레드풀)가 동시에
    // broadcast()를 호출할 수 있으므로, 같은 세션에 대한 동시 write를 원본
    // WebSocketSession으로 그대로 두면 스레드 안전하지 않다
    // (동시 sendMessage 호출 시 예외 발생 및 연결 강제 종료로 이어질 수 있음).
    // ConcurrentWebSocketSessionDecorator가 세션별로 전송을 직렬화(큐잉)해주고,
    // 느린 클라이언트가 있을 때 버퍼/시간 제한을 넘기면 그 세션만 안전하게 닫아준다.
    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
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
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.removeIf(s -> s.getId().equals(session.getId()));
        System.out.println("[WS] disconnected: " + session.getId());
    }

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
                    // 특정 세션(느리거나 끊긴 클라이언트) 전송 실패가
                    // 다른 세션 브로드캐스트를 막지 않도록 세션 단위로 격리
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