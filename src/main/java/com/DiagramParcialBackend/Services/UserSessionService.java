package com.DiagramParcialBackend.Services;

import Utils.Role;
import Utils.Status;
import com.DiagramParcialBackend.Dto.InvitationDto;
import com.DiagramParcialBackend.Entity.Session;
import com.DiagramParcialBackend.Entity.UserSession;
import com.DiagramParcialBackend.Entity.Users;
import com.DiagramParcialBackend.Repository.SessionRepository;
import com.DiagramParcialBackend.Repository.UserRepository;
import com.DiagramParcialBackend.Repository.UserSessionRepository;
import com.DiagramParcialBackend.errors.excepciones.ForbiddenException;
import com.DiagramParcialBackend.errors.excepciones.NotFoundException;
import com.DiagramParcialBackend.errors.excepciones.ResourceNotFoundException;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Data
@Service
public class UserSessionService {
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserSessionRepository userSessionRepository;


    public Users createInvitation(InvitationDto invitationDto){
        var user=this.userRepository.findById(invitationDto.getIdhost()).
                orElseThrow(() -> new NotFoundException("Host no encontrado"));
        if (user.getRole()!= Role.Host){
            throw new ForbiddenException("El usuario no tiene permisos para elimianr una sesión," +
                    " se requiere el rol de Host.");

        }

        Session session = sessionRepository.findById(invitationDto.getIdSession())
                .orElseThrow(() -> new NotFoundException("Sesión no encontrada"));

        Users collaborator = userRepository.findById(invitationDto.getIdCollaborator())
                .orElseThrow(() -> new NotFoundException("Colaborador no encontrado"));

        // Verificar si el colaborador ya está en la sesión
        Optional<UserSession> existingUserSession = userSessionRepository.findByUserAndSession(collaborator, session);
        if (existingUserSession.isPresent()) {
            throw new ResourceNotFoundException("El colaborador ya está en la sesión");
        }

        // Crear la invitación con el estado inicial PENDING
        UserSession userSession = new UserSession(collaborator, session, Status.PENDING);
        this.userSessionRepository.save(userSession);

        return collaborator;

    }
    // Aceptar una invitación (cambiar estado a ACCEPTED)
    public void acceptInvitation(InvitationDto invitationDto) {
        UserSession userSession = userSessionRepository.findByUserAndSession(
                userRepository.findById(invitationDto.getIdCollaborator())
                        .orElseThrow(() -> new NotFoundException("Usuario no encontrado")),
                sessionRepository.findById(invitationDto.getIdSession())
                        .orElseThrow(() -> new NotFoundException("Sesión no encontrada"))
        ).orElseThrow(() -> new NotFoundException("Invitación no encontrada"));

        if (userSession.getStatus() == Status.PENDING) {
            userSession.setStatus(Status.ACCEPTED);
            userSessionRepository.save(userSession);
        } else {
            throw new IllegalArgumentException("La invitación no está pendiente o ya fue gestionada.");
        }
    }

    // Rechazar una invitación (cambiar estado a REJECTED)
    public void rejectInvitation(InvitationDto invitationDto) {
        UserSession userSession = userSessionRepository.findByUserAndSession(
                userRepository.findById(invitationDto.getIdCollaborator())
                        .orElseThrow(() -> new NotFoundException("Usuario no encontrado")),
                sessionRepository.findById(invitationDto.getIdSession())
                        .orElseThrow(() -> new NotFoundException("Sesión no encontrada"))
        ).orElseThrow(() -> new NotFoundException("Invitación no encontrada"));

        if (userSession.getStatus() == Status.PENDING) {
            userSession.setStatus(Status.REJECTED);
            userSessionRepository.save(userSession);
        } else {
            throw new IllegalArgumentException("La invitación no está pendiente o ya fue gestionada.");
        }
    }

    // Ver el estado de un usuario en una sesión
    public Status getUserSessionStatus(Long userId, Long sessionId) {
        UserSession userSession = userSessionRepository.findByUserAndSession(
                userRepository.findById(userId)
                        .orElseThrow(() -> new NotFoundException("Usuario no encontrado")),
                sessionRepository.findById(sessionId)
                        .orElseThrow(() -> new NotFoundException("Sesión no encontrada"))
        ).orElseThrow(() -> new NotFoundException("Relación no encontrada"));

        return userSession.getStatus();
    }

    public List<UserSession> getAcceptedCollaboratorsInSession(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Sesión no encontrada"));

        // Filtrar por sesión y estado ACCEPTED
        return userSessionRepository.findAllBySessionAndStatus(session, Status.ACCEPTED);
    }

}
