package com.DiagramParcialBackend.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class DiagramDto {
    @NotNull(message = "Ingrese el IdSession")
    private Long sessionId;
    @NotNull(message = "No ingreso bien la data json de los diagramas")
    private Map<String, Object> data;
}
