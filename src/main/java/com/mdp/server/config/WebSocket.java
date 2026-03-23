package com.mdp.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocket implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Web / App 클라이언트가 연결할 endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
        // 필요하면 나중에 .withSockJS() 추가 가능
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 서버 -> 클라이언트 broadcast 목적지
        registry.enableSimpleBroker("/topic");

        // 클라이언트 -> 서버 전송 목적지 prefix
        registry.setApplicationDestinationPrefixes("/app");
    }
}