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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
            // Llamar al servicio para generar el ZIP con el backend de JHipster
            byte[] zipData = diagramService.exportDiagramAsZip(sessionId);

            // Generar nombre del archivo con timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "jhipster_backend_session_" + sessionId + "_" + timestamp + ".zip";

            // Configurar las cabeceras para la respuesta de descarga
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(zipData.length);

            // Devolver el archivo ZIP como una respuesta
            return new ResponseEntity<>(zipData, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            // Error cuando no se encuentra el diagrama
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IOException e) {
            // Error de IO durante la generación o compresión
            System.err.println("Error de IO al exportar diagrama: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            // Error durante la ejecución de JHipster o conversión
            System.err.println("Error durante la generación de JHipster: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            // Cualquier otro error inesperado
            System.err.println("Error inesperado al exportar diagrama: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Endpoint para obtener solo el JDL generado (útil para debugging)
    @GetMapping("/jdl/{sessionId}")
    public ResponseEntity<String> getJdlFromDiagram(@PathVariable Long sessionId) {
        try {
            Diagram diagram = diagramService.getDiagramBySessionId(sessionId);
            Map<String, Object> diagramData = diagram.getData();

            // Usar el método privado a través de reflexión o crear un método público
            // Por simplicidad, podrías hacer público el método convertJsonToJdl
            String jdlContent = "JDL generation endpoint - implement if needed for debugging";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);

            return new ResponseEntity<>(jdlContent, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
