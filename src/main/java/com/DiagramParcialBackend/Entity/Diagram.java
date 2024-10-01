package com.DiagramParcialBackend.Entity;

import com.DiagramParcialBackend.Utils.JsonConverter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;


import java.util.Map;
import java.util.Set;

@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "Diagrams", schema = "public")

public class Diagram {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;



    @CreationTimestamp // Esta anotación maneja la fecha de creación automáticamente
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.sql.Timestamp createdAt;


    @Type(JsonType.class)
    @Column(name = "data", columnDefinition = "jsonb")
    private Map<String, Object> data;

    // Relación muchos a uno con Session
    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session session;

    @OneToMany(mappedBy = "diagram", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Lock> lock;
}
