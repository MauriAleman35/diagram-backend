package com.DiagramParcialBackend.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Optional;

@Data
public class InvitationDto {


    @NotNull(message = "Ingrese la sesión que se está trabajando")
    private Long idSession;  // Cambiar a camelCase

    @NotNull(message = "Ingrese el colaborador")
    private Long idCollaborator;

}
