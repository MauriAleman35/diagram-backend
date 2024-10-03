package com.DiagramParcialBackend.Controllers;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class DiagramWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public DiagramWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/updateDiagram/{sessionId}")
    public void updateDiagram(Map<String, Object> diagramUpdate, @DestinationVariable String sessionId) {
        String eventType = (String) diagramUpdate.get("eventType");
        switch (eventType) {
            case "nodePosition":
                // Manejar actualización de posición de nodos
                Map<String, Object> updatedNode = (Map<String, Object>) diagramUpdate.get("nodeData");
                // Lógica para actualizar solo la posición del nodo
                messagingTemplate.convertAndSend("/topic/diagrams/" + sessionId, diagramUpdate);
                break;
            case "newNode":
                // Manejar creación de nuevo nodo (entidad)
                Map<String, Object> newNode = (Map<String, Object>) diagramUpdate.get("nodeData");
                // Lógica para agregar el nodo al diagrama existente
                messagingTemplate.convertAndSend("/topic/diagrams/" + sessionId, diagramUpdate);
                break;
            case "newLink":
                // Manejar creación de nueva relación (enlace)
                Map<String, Object> newLink = (Map<String, Object>) diagramUpdate.get("linkData");
                // Lógica para agregar la nueva relación
                messagingTemplate.convertAndSend("/topic/diagrams/" + sessionId, diagramUpdate);
                break;
            case "updateNode":  // Aquí se añade el manejo para la actualización de nodos
                Map<String, Object> updatedNodeData = (Map<String, Object>) diagramUpdate.get("nodeData");
                // Lógica para manejar la actualización del nodo
                messagingTemplate.convertAndSend("/topic/diagrams/" + sessionId, diagramUpdate);
                break;
            case "deleteNode": {
                Map<String, Object> deletedNodeData = (Map<String, Object>) diagramUpdate.get("nodeData");
                // Lógica para manejar la eliminación del nodo
                messagingTemplate.convertAndSend("/topic/diagrams/" + sessionId, diagramUpdate);
                break;
            }
            default:
                // Manejar otros tipos de actualizaciones si es necesario
                break;
        }
    }

}
