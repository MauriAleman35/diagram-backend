package com.DiagramParcialBackend.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SessionDto {
    @NotNull(message ="Ingrese el nombre de Session de Trabajo" )
    @Size(min = 2, message = "El Nombre debe tener almenos 2 caracteres")
    private String name;

    @NotNull(message ="Ingrese una descripcion a la Session de Trabajo" )

    private String description;


    @NotNull(message = "Ingrese el creador de esta Session")
    private Long idHost;
}
