package com.DiagramParcialBackend.Repository;

import com.DiagramParcialBackend.Entity.Diagram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiagramRepository extends JpaRepository<Diagram, Long> {

    // Buscar un diagrama por el ID de la sesión
    Optional<Diagram> findBySessionId(Long sessionId);
}