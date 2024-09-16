package com.DiagramParcialBackend.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginDto {

    @NotNull(message = "Ingrese un email")
    @Email(message = "Ingrese un correo válido")
    private String email;

    @NotNull(message = "Ingrese un codigo en el campo contrasenha")
    private String password;

}
