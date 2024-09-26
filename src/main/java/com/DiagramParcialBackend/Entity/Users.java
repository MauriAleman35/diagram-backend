package com.DiagramParcialBackend.Entity;

import com.DiagramParcialBackend.Utils.Role;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Set;

@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "User" , schema = "public")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(
            name = "email",
            length = 100,
            nullable = false
    )
    private String email;


    @Column(
            name = "name",
            length = 70,
            nullable = false
    )
    private String name;
    @Column(
            name = "password",
            length = 250,
            nullable = false
    )
    private String password;



    @OneToMany(mappedBy = "user")
    private Set<UserSession> userSessions;

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Lock> lock;
    public Users(String email,String name,String password){
        super();
        this.email=email;
        this.password=password;
        this.name=name;
    }



}
