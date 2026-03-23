package com.mdp.server.websocket;

import com.mdp.server.dto.SensorMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishSensorData(SensorMessage messageDto) {
        messagingTemplate.convertAndSend("/topic/sensor", messageDto);
    }
}