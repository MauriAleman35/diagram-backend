package com.DiagramParcialBackend.Services;



import com.DiagramParcialBackend.Dto.DiagramDto;
import com.DiagramParcialBackend.Entity.Diagram;
import com.DiagramParcialBackend.Entity.Session;
import com.DiagramParcialBackend.Repository.DiagramRepository;
import com.DiagramParcialBackend.Repository.SessionRepository;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DiagramService {

    @Autowired
    private DiagramRepository diagramRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Transactional
    public Diagram createDiagram(DiagramDto diagramDto) {
        Optional<Session> session = sessionRepository.findById(diagramDto.getSessionId());
        if (session.isPresent()) {
            // Validación del JSON
            if (diagramDto.getData() == null || diagramDto.getData().isEmpty()) {
                throw new IllegalArgumentException("La data no contiene un JSON válido.");
            }

            Diagram diagram = new Diagram();
            diagram.setSession(session.get());
            diagram.setData(diagramDto.getData()); // Se inicializa con el JSON del DTO
            return diagramRepository.save(diagram);
        } else {
            throw new IllegalArgumentException("Sesión no encontrada");
        }
    }
    @Transactional
    public Diagram updateDiagram(Long sessionId, Map<String, Object> newData) {
        Diagram diagram = diagramRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Diagrama no encontrado"));
        diagram.setData(newData);
        Diagram updatedDiagram = diagramRepository.save(diagram);

        // Notificar a los usuarios conectados al diagrama
        messagingTemplate.convertAndSend("/topic/diagrams/" + sessionId, newData);
        return updatedDiagram;
    }

    // Obtener diagrama por el ID de sesión
    public Diagram getDiagramBySessionId(Long sessionId) {
        return diagramRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Diagrama no encontrado"));
    }


    //Logica para exportar el backend
    public byte[] exportDiagramAsZip(Long sessionId) throws IOException {
        // Obtener el diagrama por el ID de sesión
        Diagram diagram = getDiagramBySessionId(sessionId);

        Map<String, Object> data = diagram.getData();
        System.out.println(data);
        // Parsear el JSON y generar las clases Java
        List<File> javaFiles = generateJavaFilesFromDiagram(data);

        // Crear un archivo ZIP
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        for (File javaFile : javaFiles) {
            ZipEntry zipEntry = new ZipEntry(javaFile.getName());
            zos.putNextEntry(zipEntry);
            FileInputStream fis = new FileInputStream(javaFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            fis.close();
            zos.closeEntry();
        }

        zos.close();
        return baos.toByteArray();
    }
    private List<File> generateJavaFilesFromDiagram(Map<String, Object> diagramData) throws IOException {
        List<File> javaFiles = new ArrayList<>();

        // Acceder a "data" dentro del JSON
        if (diagramData.containsKey("data") && diagramData.get("data") != null) {
            Map<String, Object> innerData = (Map<String, Object>) diagramData.get("data");

            // Asegurarse de que nodeDataArray exista y no sea null
            if (innerData.containsKey("nodeDataArray") && innerData.get("nodeDataArray") != null) {
                List<Map<String, Object>> nodeDataArray = (List<Map<String, Object>>) innerData.get("nodeDataArray");

                for (Map<String, Object> nodeData : nodeDataArray) {
                    String className = (String) nodeData.get("name");
                    List<Map<String, String>> attributes = (List<Map<String, String>>) nodeData.get("attributes");

                    // Generar el contenido de la clase Java
                    StringBuilder classContent = new StringBuilder();
                    classContent.append("import javax.persistence.*;\n");
                    classContent.append("@Entity\n");
                    classContent.append("public class ").append(className).append(" {\n");

                    for (Map<String, String> attribute : attributes) {
                        classContent.append("  private ").append(attribute.get("type"))
                                .append(" ").append(attribute.get("name")).append(";\n");
                    }

                    classContent.append("}\n");

                    // Escribir el archivo .java
                    File javaFile = new File(className + ".java");
                    try (FileWriter writer = new FileWriter(javaFile)) {
                        writer.write(classContent.toString());
                    }

                    javaFiles.add(javaFile);
                }
            } else {
                throw new IllegalArgumentException("El JSON no contiene nodeDataArray o es null.");
            }
        } else {
            throw new IllegalArgumentException("El JSON no contiene data o es null.");
        }

        return javaFiles;
    }


}