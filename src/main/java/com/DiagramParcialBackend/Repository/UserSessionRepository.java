package com.DiagramParcialBackend.Repository;

import Utils.Status;
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
}
