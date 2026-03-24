package com.mdp.server.websocket;

import com.mdp.server.dto.SensorMessageDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishSensorData(SensorMessageDto message) {
        // 모든 구독자에게 브로드캐스트
        messagingTemplate.convertAndSend("/topic/sensor", message);
    }
}