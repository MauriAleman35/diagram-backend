package com.DiagramParcialBackend.Controllers;



import com.DiagramParcialBackend.Dto.DiagramDto;
import com.DiagramParcialBackend.Entity.Diagram;
import com.DiagramParcialBackend.Services.DiagramService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/diagrams")
public class DiagramController {

    @Autowired
    private DiagramService diagramService;

    // Crear un nuevo diagrama al crear una sesión
    @PostMapping("/create")
    public ResponseEntity<Diagram> createDiagram(@Valid @RequestBody DiagramDto diagramDto) {
        if (!(diagramDto.getData() instanceof Map)) {
            throw new IllegalArgumentException("Data is not in JSON format");
        }
        Diagram diagram = diagramService.createDiagram(diagramDto);
        return ResponseEntity.ok(diagram);
    }
    // Actualizar un diagrama existente
    @PutMapping("/update/{sessionId}")
    public ResponseEntity<Diagram> updateDiagram(@PathVariable Long sessionId, @RequestBody Map<String, Object> data) {
        Diagram updatedDiagram = diagramService.updateDiagram(sessionId, data);
        return ResponseEntity.ok(updatedDiagram);
    }


    // Obtener diagrama por el ID de sesión
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Diagram> getDiagramBySessionId(@PathVariable Long sessionId) {
        Diagram diagram = diagramService.getDiagramBySessionId(sessionId);
        return ResponseEntity.ok(diagram);
    }
    // Nuevo endpoint para exportar el diagrama como un archivo ZIP con clases Java
    @GetMapping("/export/{sessionId}")
    public ResponseEntity<byte[]> exportDiagramAsZip(@PathVariable Long sessionId) {
        try {
            // Llamar al servicio para generar el ZIP con las clases Java
            byte[] zipData = diagramService.exportDiagramAsZip(sessionId);

            // Configurar las cabeceras para la respuesta de descarga
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "diagram_entities.zip");

            // Devolver el archivo ZIP como una respuesta
            return new ResponseEntity<>(zipData, headers, HttpStatus.OK);
        } catch (IOException e) {
            // Manejar el error en caso de problemas con la generación del ZIP
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
