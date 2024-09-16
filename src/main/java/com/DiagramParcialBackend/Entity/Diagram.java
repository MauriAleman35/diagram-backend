package com.DiagramParcialBackend.Entity;

import Utils.JsonConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
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


    @Column(
            name = "name",
            length = 70,
            nullable = false
    )
    private String name;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
    )
    private java.sql.Timestamp createdAt;


    @Convert(converter = JsonConverter.class)
    @Column(name = "data", columnDefinition = "jsonb")
    private Map<String, Object> data;


    // Relación muchos a uno con Session
    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session session;

    @OneToMany(mappedBy = "diagram", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Lock> lock;
}
