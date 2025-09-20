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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.*;
import java.nio.file.*;
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

    // Función principal para exportar el backend
    public byte[] exportDiagramAsZip(Long sessionId) throws IOException {
        System.out.println("=== INICIANDO EXPORTACIÓN DE DIAGRAMA ===");
        System.out.println("SessionId: " + sessionId);

        // 1. Obtener el diagrama
        Diagram diagram = getDiagramBySessionId(sessionId);
        Map<String, Object> diagramData = diagram.getData();
        System.out.println("Diagrama obtenido: " + (diagramData != null ? "✓" : "✗"));

        // 2. Crear directorio temporal único
        String tempDirName = "jhipster_generation_" + sessionId + "_" + System.currentTimeMillis();
        Path tempDir = Files.createTempDirectory(tempDirName);
        System.out.println("Directorio temporal creado: " + tempDir);

        try {
            // 3. Convertir JSON a JDL
            System.out.println("=== CONVIRTIENDO JSON A JDL ===");
            String jdlContent = convertJsonToJdl(diagramData);
            System.out.println("JDL generado:");
            System.out.println(jdlContent);

            // 4. Crear archivo JDL
            Path jdlFile = tempDir.resolve("model.jdl");
            Files.write(jdlFile, jdlContent.getBytes());
            System.out.println("Archivo JDL creado: " + jdlFile);

            // 5. Ejecutar JHipster
            System.out.println("=== EJECUTANDO JHIPSTER ===");
            executeJHipsterGeneration(tempDir, jdlFile);

            // 6. Generar archivos adicionales
            System.out.println("=== GENERANDO ARCHIVOS ADICIONALES ===");
            generatePostmanCollection(tempDir, diagramData);
            generateDataScript(tempDir, diagramData);

            // 7. Comprimir el resultado
            System.out.println("=== COMPRIMIENDO RESULTADO ===");
            byte[] result = compressDirectoryToZip(tempDir);
            System.out.println("ZIP generado: " + result.length + " bytes");

            return result;

        } catch (Exception e) {
            System.err.println("ERROR durante la exportación: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            // 8. Limpiar archivos temporales
            System.out.println("=== LIMPIANDO ARCHIVOS TEMPORALES ===");
            deleteDirectoryRecursively(tempDir);
        }
    }

    // Función para convertir JSON de GoJS a JDL
    private String convertJsonToJdl(Map<String, Object> diagramData) {
        StringBuilder jdl = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode rootNode = mapper.convertValue(diagramData, JsonNode.class);
            JsonNode dataNode = rootNode.get("data");

            if (dataNode == null) {
                throw new IllegalArgumentException("No se encontró el nodo 'data' en el JSON");
            }

            JsonNode nodeDataArray = dataNode.get("nodeDataArray");
            JsonNode linkDataArray = dataNode.get("linkDataArray");

            // Extraer nombres de entidades
            List<String> entityNames = new ArrayList<>();
            Map<Integer, String> keyToEntityMap = new HashMap<>();

            // Generar configuración de aplicación COMPLETAMENTE ABIERTA
            jdl.append("application {\n");
            jdl.append("  config {\n");
            jdl.append("    baseName generatorApp,\n");
            jdl.append("    applicationType monolith,\n");
            jdl.append("    packageName com.example.generator,\n");
            jdl.append("    authenticationType jwt,\n");  // Usar JWT pero configurarlo como público
            jdl.append("    databaseType sql,\n");
            jdl.append("    devDatabaseType postgresql,\n");
            jdl.append("    prodDatabaseType postgresql,\n");
            jdl.append("    skipClient true,\n");
            jdl.append("    skipUserManagement true\n");
            jdl.append("  }\n");

            // Obtener nombres de entidades
            if (nodeDataArray != null) {
                for (JsonNode node : nodeDataArray) {
                    String entityName = node.get("name").asText();
                    Integer key = node.get("key").asInt();
                    entityNames.add(entityName);
                    keyToEntityMap.put(key, entityName);
                }
            }

            jdl.append("  entities ").append(String.join(", ", entityNames)).append("\n");
            jdl.append("}\n\n");

            // Generar entidades
            if (nodeDataArray != null) {
                for (JsonNode node : nodeDataArray) {
                    String entityName = node.get("name").asText();
                    JsonNode attributes = node.get("attributes");

                    jdl.append("entity ").append(entityName).append(" {\n");

                    if (attributes != null) {
                        for (JsonNode attr : attributes) {
                            String attrName = attr.get("name").asText();
                            String attrType = attr.get("type").asText();
                            boolean isPrimaryKey = attr.get("primaryKey").asBoolean(false);
                            boolean isForeignKey = attr.get("foreignKey").asBoolean(false);

                            // Convertir tipos de GoJS a tipos JDL
                            String jdlType = convertTypeToJDL(attrType);

                            // Evitar agregar claves foráneas en la definición de entidad
                            // JHipster las manejará automáticamente con las relaciones
                            if (!isForeignKey && !attrName.equals("id")) {
                                // JHipster maneja automáticamente el ID, no necesita ser declarado
                                jdl.append("  ").append(attrName).append(" ").append(jdlType).append("\n");
                            }
                        }
                    }

                    jdl.append("}\n\n");
                }
            }

            // Generar relaciones
            if (linkDataArray != null) {
                jdl.append("// Relaciones\n");
                for (JsonNode link : linkDataArray) {
                    String relationship = link.get("relationship").asText();
                    Integer fromKey = link.get("from").asInt();
                    Integer toKey = link.get("to").asInt();
                    String fromMult = link.has("fromMult") ? link.get("fromMult").asText() : "1";
                    String toMult = link.has("toMult") ? link.get("toMult").asText() : "1";

                    String fromEntity = keyToEntityMap.get(fromKey);
                    String toEntity = keyToEntityMap.get(toKey);

                    if (fromEntity != null && toEntity != null) {
                        jdl.append("relationship ").append(relationship).append(" {\n");
                        jdl.append("  ").append(fromEntity).append(" to ").append(toEntity).append("\n");
                        jdl.append("}\n\n");
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al convertir JSON a JDL: " + e.getMessage(), e);
        }

        return jdl.toString();
    }

    // Función para convertir tipos de datos
    private String convertTypeToJDL(String goJsType) {
        switch (goJsType.toLowerCase()) {
            case "int":
            case "integer":
                return "Integer";
            case "text":
            case "string":
                return "String";
            case "decimal":
            case "float":
                return "Double";
            case "boolean":
                return "Boolean";
            case "date":
                return "LocalDate";
            case "datetime":
                return "Instant";
            default:
                return "String"; // Tipo por defecto
        }
    }

    // Función para ejecutar JHipster
    private void executeJHipsterGeneration(Path workDir, Path jdlFile) throws IOException {
        try {
            // Crear directorio para el proyecto generado
            Path projectDir = workDir.resolve("generated-project");
            Files.createDirectories(projectDir);
            System.out.println("Directorio del proyecto: " + projectDir);

            // Comando JHipster - actualizado para v8.x
            List<String> command;
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("windows")) {
                command = Arrays.asList(
                        "cmd", "/c", "jhipster", "import-jdl", jdlFile.toString(),
                        "--skip-client", "--skip-user-management", "--force"
                );
            } else {
                command = Arrays.asList(
                        "jhipster", "import-jdl", jdlFile.toString(),
                        "--skip-client", "--skip-user-management", "--force"
                );
            }

            System.out.println("Ejecutando comando: " + String.join(" ", command));
            System.out.println("En directorio: " + projectDir);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(projectDir.toFile());

            // Heredar variables de entorno del sistema (incluye PATH)
            Map<String, String> env = processBuilder.environment();
            System.out.println("PATH: " + env.get("PATH"));

            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Leer la salida del proceso
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println("JHipster OUTPUT: " + line);
                }
            }

            // Esperar a que termine el proceso
            boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("JHipster generation timed out after 10 minutes");
            }

            int exitCode = process.exitValue();
            System.out.println("JHipster exit code: " + exitCode);

            if (exitCode != 0) {
                throw new RuntimeException("JHipster generation failed with exit code: " + exitCode +
                        "\nOutput: " + output.toString());
            }

            // Verificar que se generaron archivos
            try (var stream = Files.walk(projectDir)) {
                long fileCount = stream.filter(Files::isRegularFile).count();
                System.out.println("Archivos generados: " + fileCount);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("JHipster generation was interrupted", e);
        }
    }

    // Función para comprimir directorio a ZIP
    private byte[] compressDirectoryToZip(Path sourceDir) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(path).toString());
                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException("Error al comprimir archivo: " + path, e);
                        }
                    });
        }

        return baos.toByteArray();
    }

    // Función para generar colección de Postman
    private void generatePostmanCollection(Path workDir, Map<String, Object> diagramData) throws IOException {
        StringBuilder postman = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode rootNode = mapper.convertValue(diagramData, JsonNode.class);
            JsonNode dataNode = rootNode.get("data");
            JsonNode nodeDataArray = dataNode.get("nodeDataArray");
            JsonNode linkDataArray = dataNode.get("linkDataArray");

            // Construir mapa de relaciones
            Map<String, String> relationshipMap = new HashMap<>();
            Map<String, String> entityKeys = new HashMap<>();

            if (nodeDataArray != null) {
                for (JsonNode node : nodeDataArray) {
                    Integer key = node.get("key").asInt();
                    String entityName = node.get("name").asText();
                    entityKeys.put(key.toString(), entityName);
                }
            }

            if (linkDataArray != null) {
                for (JsonNode link : linkDataArray) {
                    String relationship = link.get("relationship").asText();
                    if ("OneToMany".equals(relationship)) {
                        Integer fromKey = link.get("from").asInt();
                        Integer toKey = link.get("to").asInt();
                        String parentEntity = entityKeys.get(fromKey.toString());
                        String childEntity = entityKeys.get(toKey.toString());
                        relationshipMap.put(childEntity, parentEntity);
                    }
                }
            }

            // Inicio de la colección Postman
            postman.append("{\n");
            postman.append("  \"info\": {\n");
            postman.append("    \"name\": \"Generated API Backend\",\n");
            postman.append("    \"description\": \"API endpoints generados automáticamente - SIN AUTENTICACIÓN\",\n");
            postman.append("    \"schema\": \"https://schema.getpostman.com/json/collection/v2.1.0/collection.json\"\n");
            postman.append("  },\n");
            postman.append("  \"variable\": [\n");
            postman.append("    {\n");
            postman.append("      \"key\": \"baseUrl\",\n");
            postman.append("      \"value\": \"http://localhost:8080\"\n");
            postman.append("    }\n");
            postman.append("  ],\n");
            postman.append("  \"item\": [\n");

            boolean firstEntity = true;

            if (nodeDataArray != null) {
                for (JsonNode node : nodeDataArray) {
                    String entityName = node.get("name").asText();
                    String entityLower = entityName.toLowerCase();
                    JsonNode attributes = node.get("attributes");
                    String parentEntity = relationshipMap.get(entityName);

                    if (!firstEntity) postman.append(",\n");
                    firstEntity = false;

                    // Folder por entidad
                    postman.append("    {\n");
                    postman.append("      \"name\": \"").append(entityName).append(" API\",\n");
                    postman.append("      \"item\": [\n");

                    // GET All
                    postman.append("        {\n");
                    postman.append("          \"name\": \"Get All ").append(entityName).append("s\",\n");
                    postman.append("          \"request\": {\n");
                    postman.append("            \"method\": \"GET\",\n");
                    postman.append("            \"header\": [],\n");
                    postman.append("            \"url\": \"{{baseUrl}}/api/").append(entityLower).append("s\"\n");
                    postman.append("          }\n");
                    postman.append("        },\n");

                    // GET By ID
                    postman.append("        {\n");
                    postman.append("          \"name\": \"Get ").append(entityName).append(" by ID\",\n");
                    postman.append("          \"request\": {\n");
                    postman.append("            \"method\": \"GET\",\n");
                    postman.append("            \"header\": [],\n");
                    postman.append("            \"url\": \"{{baseUrl}}/api/").append(entityLower).append("s/1\"\n");
                    postman.append("          }\n");
                    postman.append("        },\n");

                    // POST Create con FK correcta
                    postman.append("        {\n");
                    postman.append("          \"name\": \"Create ").append(entityName).append("\",\n");
                    postman.append("          \"request\": {\n");
                    postman.append("            \"method\": \"POST\",\n");
                    postman.append("            \"header\": [\n");
                    postman.append("              {\n");
                    postman.append("                \"key\": \"Content-Type\",\n");
                    postman.append("                \"value\": \"application/json\"\n");
                    postman.append("              }\n");
                    postman.append("            ],\n");
                    postman.append("            \"body\": {\n");
                    postman.append("              \"mode\": \"raw\",\n");
                    postman.append("              \"raw\": \"{\\n");

                    // Generar body de ejemplo con FK
                    boolean firstAttr = true;
                    if (attributes != null) {
                        for (JsonNode attr : attributes) {
                            String attrName = attr.get("name").asText();
                            String attrType = attr.get("type").asText();
                            boolean isForeignKey = attr.get("foreignKey").asBoolean(false);

                            if (!attrName.equals("id")) {
                                if (!firstAttr) postman.append(",\\n");
                                firstAttr = false;

                                if (isForeignKey && parentEntity != null) {
                                    // Para FK, usar el formato estándar de JHipster
                                    String fkName = parentEntity.toLowerCase();
                                    postman.append("  \\\"").append(fkName).append("\\\": {\\n");
                                    postman.append("    \\\"id\\\": 1\\n");
                                    postman.append("  }");
                                } else {
                                    String sampleValue = getSampleValue(attrType);
                                    postman.append("  \\\"").append(attrName).append("\\\": ").append(sampleValue);
                                }
                            }
                        }
                    }

                    postman.append("\\n}\"\n");
                    postman.append("            },\n");
                    postman.append("            \"url\": \"{{baseUrl}}/api/").append(entityLower).append("s\"\n");
                    postman.append("          }\n");
                    postman.append("        },\n");

                    // PUT Update con FK
                    postman.append("        {\n");
                    postman.append("          \"name\": \"Update ").append(entityName).append("\",\n");
                    postman.append("          \"request\": {\n");
                    postman.append("            \"method\": \"PUT\",\n");
                    postman.append("            \"header\": [\n");
                    postman.append("              {\n");
                    postman.append("                \"key\": \"Content-Type\",\n");
                    postman.append("                \"value\": \"application/json\"\n");
                    postman.append("              }\n");
                    postman.append("            ],\n");
                    postman.append("            \"body\": {\n");
                    postman.append("              \"mode\": \"raw\",\n");
                    postman.append("              \"raw\": \"{\\n  \\\"id\\\": 1");

                    if (attributes != null) {
                        for (JsonNode attr : attributes) {
                            String attrName = attr.get("name").asText();
                            String attrType = attr.get("type").asText();
                            boolean isForeignKey = attr.get("foreignKey").asBoolean(false);

                            if (!attrName.equals("id")) {
                                if (isForeignKey && parentEntity != null) {
                                    String fkName = parentEntity.toLowerCase();
                                    postman.append(",\\n  \\\"").append(fkName).append("\\\": {\\n");
                                    postman.append("    \\\"id\\\": 1\\n");
                                    postman.append("  }");
                                } else {
                                    String sampleValue = getSampleValue(attrType);
                                    postman.append(",\\n  \\\"").append(attrName).append("\\\": ").append(sampleValue);
                                }
                            }
                        }
                    }

                    postman.append("\\n}\"\n");
                    postman.append("            },\n");
                    postman.append("            \"url\": \"{{baseUrl}}/api/").append(entityLower).append("s/1\"\n");
                    postman.append("          }\n");
                    postman.append("        },\n");

                    // DELETE
                    postman.append("        {\n");
                    postman.append("          \"name\": \"Delete ").append(entityName).append("\",\n");
                    postman.append("          \"request\": {\n");
                    postman.append("            \"method\": \"DELETE\",\n");
                    postman.append("            \"header\": [],\n");
                    postman.append("            \"url\": \"{{baseUrl}}/api/").append(entityLower).append("s/1\"\n");
                    postman.append("          }\n");
                    postman.append("        }\n");

                    postman.append("      ]\n");
                    postman.append("    }");
                }
            }

            postman.append("\n  ]\n");
            postman.append("}\n");

            // Escribir archivo
            Path postmanFile = workDir.resolve("API_Postman_Collection.json");
            Files.write(postmanFile, postman.toString().getBytes());
            System.out.println("Colección de Postman generada: " + postmanFile);

        } catch (Exception e) {
            throw new RuntimeException("Error al generar colección de Postman: " + e.getMessage(), e);
        }
    }

    // Función para generar script de datos
    private void generateDataScript(Path workDir, Map<String, Object> diagramData) throws IOException {
        StringBuilder sql = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode rootNode = mapper.convertValue(diagramData, JsonNode.class);
            JsonNode dataNode = rootNode.get("data");
            JsonNode nodeDataArray = dataNode.get("nodeDataArray");
            JsonNode linkDataArray = dataNode.get("linkDataArray");

            // Construir mapa de relaciones para entender dependencias
            Map<String, String> relationshipMap = new HashMap<>();
            Map<String, String> entityKeys = new HashMap<>();

            // Mapear entidades por key
            if (nodeDataArray != null) {
                for (JsonNode node : nodeDataArray) {
                    Integer key = node.get("key").asInt();
                    String entityName = node.get("name").asText();
                    entityKeys.put(key.toString(), entityName);
                }
            }

            // Mapear relaciones (OneToMany)
            if (linkDataArray != null) {
                for (JsonNode link : linkDataArray) {
                    String relationship = link.get("relationship").asText();
                    if ("OneToMany".equals(relationship)) {
                        Integer fromKey = link.get("from").asInt();
                        Integer toKey = link.get("to").asInt();
                        String parentEntity = entityKeys.get(fromKey.toString());
                        String childEntity = entityKeys.get(toKey.toString());
                        // El hijo (to) referencia al padre (from)
                        relationshipMap.put(childEntity, parentEntity);
                    }
                }
            }

            // Encabezado del script
            sql.append("-- Script para poblar la base de datos\n");
            sql.append("-- Generado automáticamente desde el diagrama\n");
            sql.append("-- IMPORTANTE: Ejecutar después de levantar la aplicación JHipster\n\n");

            // Primero insertar entidades padre (sin FK)
            List<String> processedEntities = new ArrayList<>();

            if (nodeDataArray != null) {
                // 1. Insertar primero las entidades que NO tienen FK (padres)
                for (JsonNode node : nodeDataArray) {
                    String entityName = node.get("name").asText();
                    if (!relationshipMap.containsKey(entityName)) {
                        generateEntityData(sql, node, null, relationshipMap);
                        processedEntities.add(entityName);
                    }
                }

                // 2. Luego insertar las entidades que SÍ tienen FK (hijos)
                for (JsonNode node : nodeDataArray) {
                    String entityName = node.get("name").asText();
                    if (relationshipMap.containsKey(entityName) && !processedEntities.contains(entityName)) {
                        generateEntityData(sql, node, relationshipMap.get(entityName), relationshipMap);
                        processedEntities.add(entityName);
                    }
                }
            }

            sql.append("-- Fin del script\n");
            sql.append("-- Para ejecutar: Copia y pega en tu cliente PostgreSQL\n");

            // Escribir archivo
            Path sqlFile = workDir.resolve("sample_data.sql");
            Files.write(sqlFile, sql.toString().getBytes());
            System.out.println("Script de datos generado: " + sqlFile);

        } catch (Exception e) {
            throw new RuntimeException("Error al generar script de datos: " + e.getMessage(), e);
        }
    }

    // Función auxiliar para generar datos de una entidad
    private void generateEntityData(StringBuilder sql, JsonNode node, String parentEntity, Map<String, String> relationshipMap) {
        String entityName = node.get("name").asText();
        String tableName = entityName.toLowerCase();
        JsonNode attributes = node.get("attributes");

        sql.append("-- Datos para la tabla ").append(tableName);
        if (parentEntity != null) {
            sql.append(" (referencia a ").append(parentEntity.toLowerCase()).append(")");
        }
        sql.append("\n");

        // Generar 5 registros de ejemplo por entidad
        for (int i = 1; i <= 5; i++) {
            sql.append("INSERT INTO ").append(tableName).append(" (");

            // Columnas
            boolean firstAttr = true;
            if (attributes != null) {
                for (JsonNode attr : attributes) {
                    String attrName = attr.get("name").asText();
                    boolean isForeignKey = attr.get("foreignKey").asBoolean(false);

                    if (!attrName.equals("id")) {
                        if (!firstAttr) sql.append(", ");
                        firstAttr = false;

                        // Para FK, usar el nombre estándar de JHipster
                        if (isForeignKey && parentEntity != null) {
                            sql.append(parentEntity.toLowerCase()).append("_id");
                        } else {
                            sql.append(attrName);
                        }
                    }
                }
            }

            sql.append(") VALUES (");

            // Valores
            firstAttr = true;
            if (attributes != null) {
                for (JsonNode attr : attributes) {
                    String attrName = attr.get("name").asText();
                    String attrType = attr.get("type").asText();
                    boolean isForeignKey = attr.get("foreignKey").asBoolean(false);

                    if (!attrName.equals("id")) {
                        if (!firstAttr) sql.append(", ");
                        firstAttr = false;

                        if (isForeignKey && parentEntity != null) {
                            // FK apunta a un registro del padre (1-5)
                            int parentId = (i - 1) % 5 + 1; // Distribuir entre 1-5
                            sql.append(parentId);
                        } else {
                            sql.append(getSampleSQLValue(attrType, i, attrName));
                        }
                    }
                }
            }

            sql.append(");\n");
        }

        sql.append("\n");
    }

    // Helper para generar valores de ejemplo en JSON
    private String getSampleValue(String type) {
        switch (type.toLowerCase()) {
            case "int":
            case "integer":
                return "123";
            case "text":
            case "string":
            case "varchar":
                return "\\\"Ejemplo\\\"";
            case "decimal":
            case "double":
            case "float":
                return "99.99";
            case "boolean":
                return "true";
            case "date":
                return "\\\"2024-01-01\\\"";
            case "datetime":
                return "\\\"2024-01-01T10:00:00\\\"";
            default:
                return "\\\"Valor\\\"";
        }
    }

    // Helper para generar valores de ejemplo en SQL
    private String getSampleSQLValue(String type, int index, String fieldName) {
        switch (type.toLowerCase()) {
            case "int":
            case "integer":
                return String.valueOf(index * 10);
            case "text":
            case "string":
            case "varchar":
                return "'" + fieldName + " " + index + "'";
            case "decimal":
            case "double":
            case "float":
                return String.valueOf(index * 10.99);
            case "boolean":
                return index % 2 == 0 ? "true" : "false";
            case "date":
                return "'2024-01-" + String.format("%02d", index) + "'";
            case "datetime":
                return "'2024-01-" + String.format("%02d", index) + " 10:00:00'";
            default:
                return "'" + fieldName + " " + index + "'";
        }
    }

    // Función para eliminar directorio recursivamente
    private void deleteDirectoryRecursively(Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            System.err.println("Error al eliminar directorio temporal: " + e.getMessage());
        }
    }
}