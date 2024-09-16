package com.DiagramParcialBackend.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Optional;

@Data
public class InvitationDto {

    private Long Idhost;

    @NotNull(message = "Ingrese la session que se esta trabajando")
    private Long IdSession;

    @NotNull(message = "Ingrese el colaborador")
    private Long IdCollaborator;

}
