package com.DiagramParcialBackend.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LockDto {
    @NotNull(message = "El ID del diagrama es obligatorio")
    private Long diagramId;


    private Long userId;

    @NotNull(message = "El Key de la entidad es obligatorio")
    private Long entityKey;
}
