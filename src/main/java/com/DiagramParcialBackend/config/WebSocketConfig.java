package com.DiagramParcialBackend.config;
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
        // Configuramos un broker de mensajes en la ruta /topic
        config.enableSimpleBroker("/topic");
        // Definimos prefijos para las rutas de mensajes enviados desde el cliente
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registramos el endpoint de WebSocket
        registry.addEndpoint("/ws-diagram") // Este será el punto de conexión del WebSocket
                .setAllowedOrigins("https://diagram-frontend-1er-parcial.vercel.app")
                // Permitir conexiones desde el frontend
                .withSockJS(); // Soporte para SockJS en caso de que WebSocket no esté disponible
    }
}