package com.DiagramParcialBackend.Services;

import Utils.Role;
import Utils.Status;
import com.DiagramParcialBackend.Dto.SessionDto;
import com.DiagramParcialBackend.Entity.Session;
import com.DiagramParcialBackend.Entity.UserSession;
import com.DiagramParcialBackend.Repository.SessionRepository;
import com.DiagramParcialBackend.Repository.UserRepository;
import com.DiagramParcialBackend.Repository.UserSessionRepository;
import com.DiagramParcialBackend.errors.excepciones.ForbiddenException;
import com.DiagramParcialBackend.errors.excepciones.NotFoundException;
import com.DiagramParcialBackend.errors.excepciones.ResourceNotFoundException;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Data
@Service
public class SessionService {
    //Repositorios
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserSessionRepository userSessionRepository;

    public Session createSession(SessionDto sessionDto){
        var user=this.userRepository.findById(sessionDto.getIdHost()).
                orElseThrow(() -> new NotFoundException("Host no encontrado"));;

        if (user.getRole()!= Role.Host){
            throw new ForbiddenException("El usuario no tiene permisos para crear una sesión," +
                    " se requiere el rol de Host.");

        }

        var session=new Session(sessionDto.getName());
        sessionRepository.save(session);

        //Añadir la relacion de UserSession aqui con el host
        var userseassion=new UserSession(user,session, Status.ACCEPTED);
        this.userSessionRepository.save(userseassion);

        return session;
    }

    public Session deleteSession(SessionDto sessionDto){
        var user=this.userRepository.findById(sessionDto.getIdHost()).
                orElseThrow(() -> new NotFoundException("Host no encontrado"));;
        if (user.getRole()!= Role.Host){
            throw new ForbiddenException("El usuario no tiene permisos para elimianr una sesión," +
                    " se requiere el rol de Host.");

        }

        var session=this.userSessionRepository.findById(user.getId());
        if (!session.isPresent()){
           throw new ResourceNotFoundException("Session no encontrado o no creado por el host");
        }
        var sessionDelete=this.sessionRepository.getById(session.get().getId());
        this.sessionRepository.delete(sessionDelete);

        return sessionDelete;
    }






}
