package com.framehost.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// Configures WebSocket endpoints and the STOMP message broker.
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthenticationInterceptor authenticationInterceptor;

    private final String[] allowedOrigins;

    public WebSocketConfig(
            WebSocketAuthenticationInterceptor authenticationInterceptor,
            @Value("${framehost.websocket.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") String[] allowedOrigins
    ) {
        this.authenticationInterceptor = authenticationInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    // Configures the WebSocket endpoint used by frontend clients.
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins);
    }

    // Configures destinations for client messages and room subscriptions.
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");

        registry.enableSimpleBroker("/topic");
    }

    // Authenticates connections and validates subscriptions.
    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration
    ) {
        registration.interceptors(authenticationInterceptor);
    }
}
