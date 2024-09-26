package com.DiagramParcialBackend.Services;

import com.DiagramParcialBackend.Dto.CreateInvitationDto;
import com.DiagramParcialBackend.Utils.Role;
import com.DiagramParcialBackend.Utils.Status;
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
import jakarta.transaction.Transactional;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Service
public class UserSessionService {
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserSessionRepository userSessionRepository;

    @Transactional
    public Users createInvitation(CreateInvitationDto createInvitationDto) {
        // Buscar la sesión en la que se quiere invitar
        Session session = sessionRepository.findById(createInvitationDto.getIdSession())
                .orElseThrow(() -> new NotFoundException("Sesión no encontrada"));

        // Verificar si el usuario que quiere enviar la invitación es el Host de la sesión
        Users host = userRepository.findById(createInvitationDto.getIdhost())
                .orElseThrow(() -> new NotFoundException("Host no encontrado"));

        // Verificar si el usuario es Host de esta sesión en UserSession
        Optional<UserSession> hostSession = userSessionRepository.findByUserAndSessionAndRole(host, session, Role.Host);
        if (!hostSession.isPresent()) {
            throw new ForbiddenException("El usuario no tiene permisos para enviar invitaciones, se requiere el rol de Host.");
        }




        // Verificar si el colaborador ya existe en la sesión
        Users collaborator = userRepository.findByEmail(createInvitationDto.getEmail())
                .orElseThrow(() -> new NotFoundException("Colaborador no encontrado"));

        Optional<UserSession> existingUserSession = userSessionRepository.findByUserAndSession(collaborator, session);
        if (existingUserSession.isPresent()) {
            throw new ResourceNotFoundException("El colaborador ya está en la sesión");
        }

        // Crear la invitación con el estado PENDING y el rol de Collaborator
        UserSession userSession = new UserSession(collaborator, session, Status.PENDING, Role.Collaborator);
        this.userSessionRepository.save(userSession);

        return collaborator;
    }

    public List<UserSession> getPendingInvitationsForUser(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        // Buscar todas las relaciones donde el status es PENDING y el usuario es el actual
        return userSessionRepository.findByUserAndStatus(user, Status.PENDING);
    }

    @Transactional
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

    @Transactional
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

    // Obtener todas las sesiones donde el usuario es Host
    public List<Session> getSessionsWhereUserIsHost(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        // Llamada al repositorio con el rol de Host
        List<UserSession> userSessions = userSessionRepository.findByUserAndRole(user, Role.Host);

        return userSessions.stream()
                .map(UserSession::getSession)
                .collect(Collectors.toList());
    }

    // Obtener todas las sesiones donde el usuario es Collaborator
    public List<Session> getSessionsWhereUserIsCollaborator(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        // Llamada al repositorio con el rol de Collaborator y estado ACCEPTED
        List<UserSession> userSessions = userSessionRepository.
                findByUserAndRoleAndStatus(user, Role.Collaborator, Status.ACCEPTED);

        return userSessions.stream()
                .map(UserSession::getSession)
                .collect(Collectors.toList());
    }
}
