package com.DiagramParcialBackend.Services;

import com.DiagramParcialBackend.Utils.Role;
import com.DiagramParcialBackend.Utils.Status;
import com.DiagramParcialBackend.Dto.SessionDto;
import com.DiagramParcialBackend.Entity.Session;
import com.DiagramParcialBackend.Entity.UserSession;
import com.DiagramParcialBackend.Repository.SessionRepository;
import com.DiagramParcialBackend.Repository.UserRepository;
import com.DiagramParcialBackend.Repository.UserSessionRepository;
import com.DiagramParcialBackend.errors.excepciones.ForbiddenException;
import com.DiagramParcialBackend.errors.excepciones.NotFoundException;
import com.DiagramParcialBackend.errors.excepciones.ResourceNotFoundException;
import jakarta.transaction.Transactional;
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
    @Transactional
    public Session createSession(SessionDto sessionDto){
        var user=this.userRepository.findById(sessionDto.getIdHost()).
                orElseThrow(() -> new NotFoundException("Host no encontrado"));;



        var session=new Session(sessionDto.getName(),sessionDto.getDescription());
        sessionRepository.save(session);

        //Añadir la relacion de UserSession aqui con el host

        var userseassion=new UserSession(user,session, Status.ACCEPTED,Role.Host);
        this.userSessionRepository.save(userseassion);

        return session;
    }

    @Transactional
    public Session deleteSession(Long id){
        var user=this.userRepository.findById(id).
                orElseThrow(() -> new NotFoundException("Host no encontrado"));;

        //pendiente: verificar aqui si el user es un host para poder eliminar

        var session=this.userSessionRepository.findById(user.getId());
        if (!session.isPresent()){
           throw new ResourceNotFoundException("Session no encontrado o no creado por el host");
        }
        var sessionDelete=this.sessionRepository.getById(session.get().getId());
        this.sessionRepository.delete(sessionDelete);

        return sessionDelete;
    }






}
