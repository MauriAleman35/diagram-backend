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
@Table(name = "Lock" , schema = "public")
public class Lock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
    )
    private java.sql.Timestamp createdAt;
    // Relación muchos a uno con diagram
    @ManyToOne
    @JoinColumn(name = "diagram_id")
    private Diagram diagram;
    // Relación muchos a uno con user
    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users users;


}
