package com.DiagramParcialBackend.Repository;

import com.DiagramParcialBackend.Entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session,Long> {
}
