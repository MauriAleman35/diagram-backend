package com.DiagramParcialBackend.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateInvitationDto {
    @NotNull(message = "Ingrese le host de la session")
    private Long Idhost;

    @NotNull(message = "Ingrese la session que se esta trabajando")
    private Long IdSession;

    @NotNull(message = "Ingrese el correo del colaborador para invitarlo")
    @Email(message = "Ingrese un correo válido")
    private String email;

}
