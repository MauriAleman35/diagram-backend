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
import java.util.*;
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
                    String className = (String) nodeData.get("name"); // Nombre de la clase

                    // Asegurarse de que "attributes" sea una lista de mapas
                    if (nodeData.containsKey("attributes") && nodeData.get("attributes") instanceof List) {
                        List<Map<String, Object>> rawAttributes = (List<Map<String, Object>>) nodeData.get("attributes");
                        List<Map<String, String>> attributes = new ArrayList<>();

                        // Convertir cada atributo al formato de Map<String, String>
                        for (Map<String, Object> rawAttribute : rawAttributes) {
                            Map<String, String> attribute = new HashMap<>();
                            attribute.put("name", rawAttribute.get("name").toString());
                            attribute.put("type", rawAttribute.get("type").toString());

                            attributes.add(attribute);
                        }

                        // Generar el contenido de la clase Java con anotaciones
                        String classContent = generateClassContent(className, attributes);

                        // Escribir el archivo .java
                        File javaFile = new File(className + ".java");
                        try (FileWriter writer = new FileWriter(javaFile)) {
                            writer.write(classContent);
                        }

                        javaFiles.add(javaFile);
                    } else {
                        throw new IllegalArgumentException("El nodo no contiene 'attributes' o no es una lista.");
                    }
                }
            } else {
                throw new IllegalArgumentException("El JSON no contiene nodeDataArray o es null.");
            }
        } else {
            throw new IllegalArgumentException("El JSON no contiene data o es null.");
        }

        return javaFiles;
    }


    private String generateClassContent(String className, List<Map<String, String>> attributes) {
        StringBuilder classContent = new StringBuilder();

        // Agregar las anotaciones principales
        classContent.append("import javax.persistence.*;\n")
                .append("import lombok.*;\n\n")
                .append("@Data\n")
                .append("@ToString\n")
                .append("@EqualsAndHashCode\n")
                .append("@NoArgsConstructor\n")
                .append("@Entity\n")
                .append("@Table(name = \"").append(className).append("\", schema = \"public\")\n")
                .append("public class ").append(className).append(" {\n\n");

        // Añadir los atributos de la clase
        for (Map<String, String> attribute : attributes) {
            String attrName = attribute.get("name");
            String attrType = attribute.get("type");

            // Si el nombre del atributo contiene "id", agregar solo @Id y @GeneratedValue
            if (attrName.toLowerCase().contains("id")) {
                classContent.append("    @Id\n")
                        .append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n")
                        .append("    private ").append(mapToJavaType(attrType)).append(" ").append(attrName).append(";\n\n");
            } else {
                // Para otros atributos, agregar la anotación @Column
                classContent.append("    @Column(name = \"").append(attrName).append("\", nullable = false)\n")
                        .append("    private ").append(mapToJavaType(attrType)).append(" ").append(attrName).append(";\n\n");
            }
        }

        // Constructor adicional si tiene más atributos además del ID
        classContent.append("    public ").append(className).append("(");
        StringJoiner constructorParams = new StringJoiner(", ");
        StringJoiner constructorBody = new StringJoiner("\n");

        for (Map<String, String> attribute : attributes) {
            String attrName = attribute.get("name");
            if (!attrName.toLowerCase().contains("id")) { // Excluir los ID del constructor
                String attrType = attribute.get("type");

                constructorParams.add(mapToJavaType(attrType) + " " + attrName);
                constructorBody.add("        this." + attrName + " = " + attrName + ";");
            }
        }

        classContent.append(constructorParams.toString()).append(") {\n")
                .append("        super();\n")
                .append(constructorBody.toString()).append("\n")
                .append("    }\n");

        // Cerrar la clase
        classContent.append("}\n");

        return classContent.toString();
    }



    private String mapToJavaType(String type) {
        switch (type.toLowerCase()) {
            case "int":
                return "Integer";
            case "varchar":
                return "String";
            case "boolean":
                return "Boolean";
            case "datetime":
                return "LocalDateTime";
            default:
                return "String";  // Default a String si el tipo no está claro
        }
    }



}