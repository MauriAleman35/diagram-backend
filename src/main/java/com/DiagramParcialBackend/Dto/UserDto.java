package com.DiagramParcialBackend.Dto;

import com.DiagramParcialBackend.Utils.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDto {
    @NotNull(message = "Ingrese un nombre")
    @Size(min = 2, message = "El nombre debe tener al menos 2 caracteres")
    private String name;

    @NotNull(message = "Ingrese un email")
    @Email(message = "Ingrese un correo válido")
    private String email;

    @NotNull(message = "Introduzca una contraseña")
    private String password;


}
