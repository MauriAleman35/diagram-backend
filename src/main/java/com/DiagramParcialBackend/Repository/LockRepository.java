package com.DiagramParcialBackend.Repository;


import com.DiagramParcialBackend.Entity.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
public interface LockRepository extends JpaRepository<Lock, Long> {

    // Cambia para que utilice el ID del diagrama en lugar de la entidad completa
    Optional<Lock> findByDiagram_IdAndEntityKey(Long diagramId, Long entityKey);

    // Cambia para que verifique si existe un lock con el ID del diagrama y la key de la entidad
    boolean existsByDiagram_IdAndEntityKey(Long diagramId, Long entityKey);
}