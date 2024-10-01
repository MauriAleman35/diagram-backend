package com.DiagramParcialBackend.Controllers;



import com.DiagramParcialBackend.Dto.DiagramDto;
import com.DiagramParcialBackend.Entity.Diagram;
import com.DiagramParcialBackend.Services.DiagramService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
