package com.DiagramParcialBackend.Controllers;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class DiagramWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public DiagramWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Escucha los mensajes enviados desde el frontend a /app/updateDiagram
    @MessageMapping("/updateDiagram/{sessionId}")
    public void updateDiagram(String diagramJson, @DestinationVariable String sessionId) {
        // Enviar el diagrama actualizado a todos los suscriptores de esta sesión
        messagingTemplate.convertAndSend("/topic/diagrams/" + sessionId, diagramJson);
    }
}
