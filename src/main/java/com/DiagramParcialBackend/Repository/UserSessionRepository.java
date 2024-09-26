package com.DiagramParcialBackend.Repository;

import com.DiagramParcialBackend.Utils.Role;
import com.DiagramParcialBackend.Utils.Status;
import com.DiagramParcialBackend.Entity.Session;
import com.DiagramParcialBackend.Entity.UserSession;
import com.DiagramParcialBackend.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession,Long> {

    // Buscar una relación entre un usuario y una sesión específica
    Optional<UserSession> findByUserAndSession(Users user, Session session);

    // Obtener todas las relaciones para una sesión
    List<UserSession> findAllBySession(Session session);

    List<UserSession> findAllBySessionAndStatus(Session session, Status status);

    Optional<UserSession> findByUserAndSessionAndRole(Users user, Session session, Role role);



    //Consultas para obtener las sessiones como colaborador o host

    // Obtener todas las sesiones donde el usuario es Host
    List<UserSession> findByUserAndRole(Users user, Role role);

    // Obtener todas las sesiones donde el usuario es Collaborator y la invitación ha sido aceptada
    List<UserSession> findByUserAndRoleAndStatus(Users user, Role role, Status status);



    // Obtener todas las relaciones de usuario-sesión donde el status es PENDING
    List<UserSession> findByUserAndStatus(Users user, Status status);

}
