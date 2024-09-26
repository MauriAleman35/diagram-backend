package com.DiagramParcialBackend.Controllers;

import com.DiagramParcialBackend.Dto.SessionDto;
import com.DiagramParcialBackend.Dto.UserDto;
import com.DiagramParcialBackend.Entity.Session;
import com.DiagramParcialBackend.Repository.SessionRepository;
import com.DiagramParcialBackend.Repository.UserSessionRepository;
import com.DiagramParcialBackend.Response.ApiResponse;
import com.DiagramParcialBackend.Response.SessionResponse;
import com.DiagramParcialBackend.Services.SessionService;
import com.DiagramParcialBackend.Services.UserSessionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/session")
public class SessionController {
    //Inyecciones de dependencias de Repositorios
    @Autowired
    SessionRepository sessionRepository;

    @Autowired
    UserSessionRepository userSessionRepository;

    //Inyecciones de dependencias de Servicios
    @Autowired
    UserSessionService userSessionService;
    @Autowired
    SessionService sessionService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<SessionResponse> postSession(@Valid @RequestBody SessionDto sessionDto){
        return new ApiResponse<>(
                HttpStatus.CREATED.value(),
                "Session creada con exito",
                new SessionResponse(sessionService.createSession(sessionDto))
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Session deleteSession(@PathVariable Long id){
        return this.sessionService.deleteSession(id);
    }

}
