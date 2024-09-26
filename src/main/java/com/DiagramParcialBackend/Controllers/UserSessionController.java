package com.DiagramParcialBackend.Controllers;

import com.DiagramParcialBackend.Dto.CreateInvitationDto;
import com.DiagramParcialBackend.Dto.InvitationDto;
import com.DiagramParcialBackend.Entity.Session;
import com.DiagramParcialBackend.Entity.UserSession;
import com.DiagramParcialBackend.Entity.Users;
import com.DiagramParcialBackend.Response.ApiResponse;
import com.DiagramParcialBackend.Response.UserResponse;
import com.DiagramParcialBackend.Services.UserSessionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user-session")
public class UserSessionController {

    @Autowired
    private UserSessionService userSessionService;

    // Enviar una invitación a un colaborador
    @PostMapping("/invite")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<UserResponse> createInvitation(@Valid  @RequestBody CreateInvitationDto createInvitationDto) {
        return new ApiResponse<>(
                HttpStatus.CREATED.value(),
                "Invitacion enviada con exito",
                new UserResponse(this.userSessionService.createInvitation(createInvitationDto))
        );
    }

    // Aceptar una invitación
    @PostMapping("/accept")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<Void> acceptInvitation( @Valid @RequestBody InvitationDto invitationDto) {
        userSessionService.acceptInvitation(invitationDto);
        return ResponseEntity.ok().build();
    }

    // Rechazar una invitación
    @PostMapping("/reject")
    public ResponseEntity<Void> rejectInvitation(@RequestBody InvitationDto invitationDto) {
        userSessionService.rejectInvitation(invitationDto);
        return ResponseEntity.ok().build();
    }


    // Obtener todos los colaboradores aceptados en una sesión
    //Revisar
    @GetMapping("/collaborators/{sessionId}")
    public ResponseEntity<List<UserSession>> getAcceptedCollaboratorsInSession(@PathVariable Long sessionId) {
        List<UserSession> collaborators = userSessionService.getAcceptedCollaboratorsInSession(sessionId);
        return ResponseEntity.ok(collaborators);
    }

    // Obtener todas las sesiones donde el usuario es Host
    @GetMapping("/{userId}/host-sessions")
    public ResponseEntity<List<Session>> getSessionsWhereUserIsHost(@PathVariable Long userId) {
        List<Session> sessions = userSessionService.getSessionsWhereUserIsHost(userId);
        return ResponseEntity.ok(sessions);
    }

    // Obtener todas las sesiones donde el usuario es Collaborator
    @GetMapping("/{userId}/collaborator-sessions")
    public ResponseEntity<List<Session>> getSessionsWhereUserIsCollaborator(@PathVariable Long userId) {
        List<Session> sessions = userSessionService.getSessionsWhereUserIsCollaborator(userId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{userId}/pending-invitations")
    public ResponseEntity<List<UserSession>> getPendingInvitations(@PathVariable Long userId) {
        List<UserSession> pendingInvitations = userSessionService.getPendingInvitationsForUser(userId);
        return ResponseEntity.ok(pendingInvitations);
    }
}
