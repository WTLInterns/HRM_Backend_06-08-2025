package com.jaywant.demo.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

        @Override
        public void configureMessageBroker(MessageBrokerRegistry config) {
                config.enableSimpleBroker("/topic");
                config.setApplicationDestinationPrefixes("/app");
        }

        @Override
        public void registerStompEndpoints(StompEndpointRegistry registry) {
                // Register the "/ws" endpoint, enabling the SockJS protocol
                // SockJS is used to enable fallback options for browsers that don't support
                // WebSocket
                registry.addEndpoint("/ws")
                                .setAllowedOrigins(
                                                "http://localhost:5173",
                                                "http://localhost:5174",
                                                "https://admin.managifyhr.com",
                                                "http://localhost:8080")
                                .withSockJS();

                registry.addEndpoint("/ws")
                                .setAllowedOrigins(
                                                "http://localhost:5173",
                                                "http://localhost:5174",
                                                "https://admin.managifyhr.com",
                                                "http://localhost:8080");

        }
}
