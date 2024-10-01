package com.DiagramParcialBackend.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "Lock", schema = "public")
public class Lock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // Relación muchos a uno con Diagram
    @ManyToOne
    @JoinColumn(name = "diagram_id", nullable = false)
    private Diagram diagram;

    // Relación muchos a uno con User
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    // Clave única de la entidad que está siendo bloqueada
    @Column(name = "entity_key", nullable = false)
    private Long entityKey;  // Esta es la clave de la entidad en el JSON (key)

    // Constructor con parámetros
    public Lock(Diagram diagram, Users user, Long entityKey) {
        this.diagram = diagram;
        this.user = user;
        this.entityKey = entityKey;
    }
}
