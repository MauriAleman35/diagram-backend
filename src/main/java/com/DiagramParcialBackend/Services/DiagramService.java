package com.DiagramParcialBackend.Services;



import com.DiagramParcialBackend.Dto.DiagramDto;
import com.DiagramParcialBackend.Entity.Diagram;
import com.DiagramParcialBackend.Entity.Session;
import com.DiagramParcialBackend.Repository.DiagramRepository;
import com.DiagramParcialBackend.Repository.SessionRepository;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class DiagramService {

    @Autowired
    private DiagramRepository diagramRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Transactional
    public Diagram createDiagram(DiagramDto diagramDto) {
        Optional<Session> session = sessionRepository.findById(diagramDto.getSessionId());
        if (session.isPresent()) {
            // Validación del JSON
            if (diagramDto.getData() == null || diagramDto.getData().isEmpty()) {
                throw new IllegalArgumentException("La data no contiene un JSON válido.");
            }

            Diagram diagram = new Diagram();
            diagram.setSession(session.get());
            diagram.setData(diagramDto.getData()); // Se inicializa con el JSON del DTO
            return diagramRepository.save(diagram);
        } else {
            throw new IllegalArgumentException("Sesión no encontrada");
        }
    }
    @Transactional
    public Diagram updateDiagram(Long sessionId, Map<String, Object> newData) {
        Diagram diagram = diagramRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Diagrama no encontrado"));
        diagram.setData(newData);
        Diagram updatedDiagram = diagramRepository.save(diagram);

        // Notificar a los usuarios conectados al diagrama
        messagingTemplate.convertAndSend("/topic/diagrams/" + sessionId, newData);
        return updatedDiagram;
    }

    // Obtener diagrama por el ID de sesión
    public Diagram getDiagramBySessionId(Long sessionId) {
        return diagramRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Diagrama no encontrado"));
    }
}