package com.DiagramParcialBackend.Services;

import com.DiagramParcialBackend.Dto.LockDto;
import com.DiagramParcialBackend.Dto.LockMessage;
import com.DiagramParcialBackend.Entity.Diagram;
import com.DiagramParcialBackend.Entity.Lock;
import com.DiagramParcialBackend.Entity.Users;

import com.DiagramParcialBackend.Repository.DiagramRepository;
import com.DiagramParcialBackend.Repository.LockRepository;
import com.DiagramParcialBackend.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LockService {

    @Autowired
    private LockRepository lockRepository;

    @Autowired
    private DiagramRepository diagramRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Crear un nuevo bloqueo para una entidad específica
    @Transactional
    public Lock createLock(LockDto lockDto) {
        Optional<Diagram> diagram = diagramRepository.findById(lockDto.getDiagramId());
        Optional<Users> user = userRepository.findById(lockDto.getUserId());

        if (diagram.isPresent() && user.isPresent()) {
            // Verifica si ya está bloqueada
            Optional<Lock> existingLock = lockRepository.findByDiagram_IdAndEntityKey(diagram.get().getId(), lockDto.getEntityKey());
            if (existingLock.isPresent()) {
                throw new IllegalArgumentException("La entidad ya está bloqueada por otro usuario.");
            }

            // Crear el nuevo lock
            Lock lock = new Lock(diagram.get(), user.get(), lockDto.getEntityKey());
            Lock savedLock = lockRepository.save(lock);

            // Notificar a través de WebSocket
            LockMessage lockMessage = new LockMessage(lockDto.getUserId(), lockDto.getEntityKey(), "locked");
            messagingTemplate.convertAndSend("/topic/lockStatus/" + diagram.get().getId(), lockMessage);

            return savedLock;
        } else {
            throw new IllegalArgumentException("Diagrama o Usuario no encontrado");
        }
    }

    // Liberar el bloqueo de una entidad
    @Transactional
    public void releaseLock(LockDto lockDto) {
        // Verificar si el lock existe basado en diagramId y entityKey
        Optional<Lock> lock = lockRepository.findByDiagram_IdAndEntityKey(lockDto.getDiagramId(), lockDto.getEntityKey());

        // Si el lock está presente, se procede a eliminarlo
        if (lock.isPresent()) {
            lockRepository.delete(lock.get());

            // Notificar a través de WebSocket sobre la liberación del bloqueo
            LockMessage lockMessage = new LockMessage(lockDto.getUserId(), lockDto.getEntityKey(), "unlocked");
            messagingTemplate.convertAndSend("/topic/lockStatus/" + lockDto.getDiagramId(), lockMessage);
        } else {
            throw new IllegalArgumentException("No existe bloqueo para esa entidad en el diagrama");
        }
    }

    // Verificar si una entidad está bloqueada
    public boolean isLocked(Long diagramId, Long entityKey) {
        return lockRepository.existsByDiagram_IdAndEntityKey(diagramId, entityKey);
    }
}
