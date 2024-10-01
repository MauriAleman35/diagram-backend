package com.DiagramParcialBackend.Controllers;

import com.DiagramParcialBackend.Dto.DiagramMessage;
import com.DiagramParcialBackend.Dto.LockMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class DiagramWebSocketController {

    // Cuando se recibe un mensaje para actualizar el diagrama
    @MessageMapping("/updateDiagram")
    @SendTo("/topic/diagrams/{sessionId}")
    public Map<String, Object> updateDiagram(DiagramMessage message) {
        // Notificar a todos los usuarios sobre el nuevo estado del diagrama
        return message.getData();
    }

    // Cuando un usuario bloquea una entidad
    @MessageMapping("/lockEntity")
    @SendTo("/topic/lockStatus/{sessionId}")
    public LockMessage lockEntity(LockMessage lockMessage) {
        // Notificar a todos que una entidad está bloqueada
        return lockMessage;
    }

    // Cuando un usuario libera una entidad
    @MessageMapping("/unlockEntity")
    @SendTo("/topic/lockStatus/{sessionId}")
    public LockMessage unlockEntity(LockMessage lockMessage) {
        // Notificar a todos que una entidad fue liberada
        return lockMessage;
    }
}
