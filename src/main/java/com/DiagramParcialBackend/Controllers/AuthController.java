package com.DiagramParcialBackend.Controllers;

import com.DiagramParcialBackend.Dto.LoginDto;
import com.DiagramParcialBackend.Response.ApiResponse;
import com.DiagramParcialBackend.Response.AuthResponse;
import com.DiagramParcialBackend.Services.Security.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "auth")

public class AuthController {
    @Autowired
    AuthService authService;

    @PostMapping("login")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<AuthResponse> postMethodName(@Valid @RequestBody LoginDto loginDto) {
        return new ApiResponse<>(
                HttpStatus.CREATED.value(),
                "Logeado correctamente",
                authService.login(loginDto)
        );
    }

}

