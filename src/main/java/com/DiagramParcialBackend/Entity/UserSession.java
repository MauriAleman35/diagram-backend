package com.DiagramParcialBackend.Entity;

import com.DiagramParcialBackend.Utils.Role;
import com.DiagramParcialBackend.Utils.Status;
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
@Table(name = "user_sessions")
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Status status;
    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session session;


    public UserSession(Users users, Session session, Status status,Role role){
        this.user=users;
        this.session=session;
        this.status=status;
        this.role=role;
    }
}