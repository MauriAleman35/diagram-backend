package com.DiagramParcialBackend.Entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Set;


@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "Session" , schema = "public")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "name",
            length = 70,
            nullable = false
    )
    private String name;

    @Column(
            name = "description",
            length = 150,
            nullable = false
    )
    private String description;

    @CreationTimestamp // Esta anotación maneja la fecha de creación automáticamente
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.sql.Timestamp createdAt;

    @OneToMany(mappedBy = "session")
    private Set<UserSession> userSessions;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Diagram> diagrams;
    public Session(String name, String description){
        super();
        this.name=name;
        this.description=description;

    }
}
