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
import java.util.concurrent.TimeUnit;
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

    // -------------------- CRUD de Diagram --------------------

    @Transactional
    public Diagram createDiagram(DiagramDto diagramDto) {
        Optional<Session> session = sessionRepository.findById(diagramDto.getSessionId());
        if (session.isEmpty()) throw new IllegalArgumentException("Sesión no encontrada");
        if (diagramDto.getData() == null || diagramDto.getData().isEmpty()) {
            throw new IllegalArgumentException("La data no contiene un JSON válido.");
        }
        Diagram diagram = new Diagram();
        diagram.setSession(session.get());
        diagram.setData(diagramDto.getData());
        return diagramRepository.save(diagram);
    }

    @Transactional
    public Diagram updateDiagram(Long sessionId, Map<String, Object> newData) {
        Diagram diagram = diagramRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Diagrama no encontrado"));
        diagram.setData(newData);
        Diagram updatedDiagram = diagramRepository.save(diagram);
        messagingTemplate.convertAndSend("/topic/diagrams/" + sessionId, newData);
        return updatedDiagram;
    }

    public Diagram getDiagramBySessionId(Long sessionId) {
        return diagramRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Diagrama no encontrado"));
    }

    // -------------------- Exportación --------------------

    public byte[] exportDiagramAsZip(Long sessionId) throws IOException {
        System.out.println("=== INICIANDO EXPORTACIÓN DE DIAGRAMA ===");
        System.out.println("SessionId: " + sessionId);

        Diagram diagram = getDiagramBySessionId(sessionId);
        Map<String, Object> diagramData = diagram.getData();
        System.out.println("Diagrama obtenido: " + (diagramData != null ? "✓" : "✗"));

        String tempDirName = "jhipster_generation_" + sessionId + "_" + System.nanoTime();
        Path tempDir = Files.createTempDirectory(tempDirName);
        System.out.println("Directorio temporal creado: " + tempDir);

        try {
            // 1) JSON -> JDL
            System.out.println("=== CONVIRTIENDO JSON A JDL ===");
            String jdlContent = convertJsonToJdl(diagramData);
            System.out.println("JDL generado:");
            System.out.println(jdlContent);
            Path jdlFile = tempDir.resolve("model.jdl");
            Files.writeString(jdlFile, jdlContent);
            System.out.println("Archivo JDL creado: " + jdlFile);

            // 2) Ejecutar JHipster
            System.out.println("=== EJECUTANDO JHIPSTER ===");
            Path projectDir = executeJHipsterGeneration(tempDir, jdlFile);

            // 3) Archivos adicionales
            System.out.println("=== GENERANDO ARCHIVOS ADICIONALES ===");
            generatePostmanCollection(tempDir, diagramData);
            generateDataScript(tempDir, diagramData);

            // 4) Limpiar directorios pesados antes de comprimir
            deleteHeavyDirs(projectDir);

            // 5) Comprimir (con exclusiones)
            System.out.println("=== COMPRIMIENDO RESULTADO ===");
            byte[] result = compressDirectoryToZip(tempDir);
            System.out.println("ZIP generado: " + result.length + " bytes");

            return result;

        } catch (Exception e) {
            System.err.println("ERROR durante la exportación: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            System.out.println("=== LIMPIANDO ARCHIVOS TEMPORALES ===");
            deleteDirectoryRecursively(tempDir);
        }
    }

    // -------------------- JSON (GoJS) -> JDL con detección M:N mejorada --------------------

    private static class Attr {
        String name, type, referencedEntity;
        boolean primaryKey, foreignKey;
        int referencedKey; // Para mapear correctamente las FK
    }

    private static class NodeInfo {
        String name;
        int key;
        List<Attr> attrs = new ArrayList<>();
        boolean isIntermediate;
    }

    private String convertJsonToJdl(Map<String, Object> diagramData) {
        StringBuilder jdl = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode rootNode = mapper.convertValue(diagramData, JsonNode.class);
            JsonNode dataNode = rootNode.get("data");
            if (dataNode == null) throw new IllegalArgumentException("No se encontró el nodo 'data' en el JSON");

            JsonNode nodeDataArray = dataNode.get("nodeDataArray");
            JsonNode linkDataArray = dataNode.get("linkDataArray");

            // ---- Parsear nodos y atributos
            Map<Integer, NodeInfo> nodes = new HashMap<>();
            Map<Integer, String> keyToEntity = new HashMap<>();
            if (nodeDataArray != null) {
                for (JsonNode n : nodeDataArray) {
                    NodeInfo ni = new NodeInfo();
                    ni.key = n.get("key").asInt();
                    ni.name = n.get("name").asText();
                    ni.isIntermediate = n.has("isIntermediateTable") && n.get("isIntermediateTable").asBoolean();

                    if (n.has("attributes") && n.get("attributes").isArray()) {
                        for (JsonNode a : n.get("attributes")) {
                            Attr at = new Attr();
                            at.name = a.get("name").asText();
                            at.type = a.get("type").asText();
                            at.primaryKey = a.has("primaryKey") && a.get("primaryKey").asBoolean();
                            at.foreignKey = a.has("foreignKey") && a.get("foreignKey").asBoolean();
                            if (a.has("referencedEntity")) at.referencedEntity = a.get("referencedEntity").asText();
                            if (a.has("referencedKey")) at.referencedKey = a.get("referencedKey").asInt();
                            ni.attrs.add(at);
                        }
                    }

                    nodes.put(ni.key, ni);
                    keyToEntity.put(ni.key, ni.name);
                }
            }

            // ---- Mejorar detección de tablas intermedias y pure joins
            Set<String> pureJoinPairs = new HashSet<>();       // genera ManyToMany y NO entidad
            Set<String> associationEntities = new HashSet<>(); // join con campos extra -> entidad + 2 ManyToOne
            Map<String, List<String>> intermediateToEntities = new HashMap<>(); // mapeo de tabla intermedia a entidades relacionadas

            for (NodeInfo ni : nodes.values()) {
                List<Attr> foreignKeys = ni.attrs.stream()
                        .filter(a -> a.foreignKey && a.referencedEntity != null)
                        .toList();

                long nonFkNonIdCount = ni.attrs.stream()
                        .filter(a -> !a.foreignKey && !"id".equalsIgnoreCase(a.name))
                        .count();

                boolean looksLikeJoin = (foreignKeys.size() == 2);

                if (ni.isIntermediate || looksLikeJoin) {
                    List<String> referencedEntities = foreignKeys.stream()
                            .map(a -> a.referencedEntity)
                            .distinct()
                            .sorted()
                            .toList();

                    if (referencedEntities.size() == 2) {
                        intermediateToEntities.put(ni.name, referencedEntities);

                        // Si tiene campos extra (además de las FKs y el id), es association entity
                        if (nonFkNonIdCount > 0) {
                            associationEntities.add(ni.name);
                            System.out.println("Detectada association entity: " + ni.name + " con campos extra");
                        } else {
                            // Pure join: solo las dos FKs (y posiblemente un id autogenerado)
                            String pairKey = referencedEntities.get(0) + "::" + referencedEntities.get(1);
                            pureJoinPairs.add(pairKey);
                            System.out.println("Detectada pure join: " + ni.name + " -> " + pairKey);
                        }
                    }
                }
            }

            // ---- Cabecera de aplicación
            jdl.append("application {\n");
            jdl.append("  config {\n");
            jdl.append("    baseName generatorApp,\n");
            jdl.append("    applicationType monolith,\n");
            jdl.append("    packageName com.example.generator,\n");
            jdl.append("    authenticationType jwt,\n");
            jdl.append("    databaseType sql,\n");
            jdl.append("    devDatabaseType postgresql,\n");
            jdl.append("    prodDatabaseType postgresql,\n");
            jdl.append("    skipClient true,\n");
            jdl.append("    skipUserManagement true\n");
            jdl.append("  }\n");

            // Entidades listadas: todas menos pure joins
            List<String> entityNames = new ArrayList<>();
            for (NodeInfo ni : nodes.values()) {
                boolean isPureJoin = isPureJoinNode(ni, pureJoinPairs);
                if (!isPureJoin) {
                    entityNames.add(ni.name);
                }
            }
            jdl.append("  entities ").append(String.join(", ", entityNames)).append("\n");
            jdl.append("}\n\n");

            // ---- Definición de entidades
            for (NodeInfo ni : nodes.values()) {
                if (isPureJoinNode(ni, pureJoinPairs)) {
                    System.out.println("Saltando pure join entity: " + ni.name);
                    continue;
                }

                jdl.append("entity ").append(ni.name).append(" {\n");

                // Para association entities, manejar las FKs de manera especial
                if (associationEntities.contains(ni.name)) {
                    // No incluir las FK como campos simples, se manejan como relaciones
                    for (Attr a : ni.attrs) {
                        if (a.foreignKey) continue; // Las FKs se manejan como relaciones
                        if ("id".equalsIgnoreCase(a.name)) continue; // JHipster crea id automático
                        String jdlType = convertTypeToJDL(a.type);
                        jdl.append("  ").append(a.name).append(" ").append(jdlType).append("\n");
                    }
                } else {
                    // Entidad normal
                    for (Attr a : ni.attrs) {
                        if (a.foreignKey) continue; // Las relaciones se definen después
                        if ("id".equalsIgnoreCase(a.name)) continue; // JHipster crea id automático
                        String jdlType = convertTypeToJDL(a.type);
                        jdl.append("  ").append(a.name).append(" ").append(jdlType).append("\n");
                    }
                }

                jdl.append("}\n\n");
            }

            // ---- Relaciones
            jdl.append("// Relaciones\n");

            // 1) ManyToMany para pure joins (tablas intermedias sin campos extra)
            for (String pair : pureJoinPairs) {
                String[] ends = pair.split("::");
                jdl.append("relationship ManyToMany {\n");
                jdl.append("  ").append(ends[0]).append(" to ").append(ends[1]).append("\n");
                jdl.append("}\n\n");
            }

            // 2) Association entities -> dos ManyToOne hacia las entidades relacionadas
            for (String assocEntity : associationEntities) {
                List<String> relatedEntities = intermediateToEntities.get(assocEntity);
                if (relatedEntities != null && relatedEntities.size() == 2) {
                    // Buscar los nombres de los campos FK para usar como nombres de relación
                    NodeInfo assocNode = nodes.values().stream()
                            .filter(n -> n.name.equals(assocEntity))
                            .findFirst()
                            .orElse(null);

                    if (assocNode != null) {
                        for (String targetEntity : relatedEntities) {
                            // Buscar el atributo FK que apunta a esta entidad
                            String fieldName = assocNode.attrs.stream()
                                    .filter(a -> a.foreignKey && targetEntity.equals(a.referencedEntity))
                                    .map(a -> {
                                        // Convertir idProducto -> producto, idPedido -> pedido
                                        String name = a.name;
                                        if (name.toLowerCase().startsWith("id")) {
                                            name = name.substring(2);
                                        }
                                        return name.toLowerCase();
                                    })
                                    .findFirst()
                                    .orElse(targetEntity.toLowerCase());

                            jdl.append("relationship ManyToOne {\n");
                            jdl.append("  ").append(assocEntity).append("{").append(fieldName).append("} to ").append(targetEntity).append("\n");
                            jdl.append("}\n\n");
                        }
                    }
                }
            }

            // 3) Otras relaciones del canvas (evitando duplicadas con ManyToMany y association entities)
            if (linkDataArray != null) {
                for (JsonNode link : linkDataArray) {
                    if (!link.has("from") || !link.has("to")) continue;
                    int fromKey = link.get("from").asInt();
                    int toKey = link.get("to").asInt();
                    String rel = link.has("relationship") ? link.get("relationship").asText() : null;

                    String fromEntity = keyToEntity.get(fromKey);
                    String toEntity = keyToEntity.get(toKey);
                    if (fromEntity == null || toEntity == null) continue;

                    // Saltear si hay marcador isManyToManyPart (indica que es parte de M:N)
                    if (link.has("isManyToManyPart") && link.get("isManyToManyPart").asBoolean()) {
                        continue;
                    }

                    // Si alguna entidad es association entity, ya generamos sus relaciones
                    if (associationEntities.contains(fromEntity) || associationEntities.contains(toEntity)) {
                        continue;
                    }

                    // Si este par está cubierto por ManyToMany, skip
                    if (pureJoinPairs.contains(pairKey(fromEntity, toEntity))) {
                        continue;
                    }

                    // Si alguna entidad no existe (fue pure join), skip
                    if (!entityNames.contains(fromEntity) || !entityNames.contains(toEntity)) {
                        continue;
                    }

                    String validRel = convertToValidJDLRelationship(rel);
                    if (validRel == null) continue;

                    jdl.append("relationship ").append(validRel).append(" {\n");
                    jdl.append("  ").append(fromEntity).append(" to ").append(toEntity).append("\n");
                    jdl.append("}\n\n");
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al convertir JSON a JDL: " + e.getMessage(), e);
        }

        return jdl.toString();
    }

    private boolean isPureJoinNode(NodeInfo ni, Set<String> pureJoinPairs) {
        List<Attr> foreignKeys = ni.attrs.stream()
                .filter(a -> a.foreignKey && a.referencedEntity != null)
                .toList();

        long nonFkNonIdCount = ni.attrs.stream()
                .filter(a -> !a.foreignKey && !"id".equalsIgnoreCase(a.name))
                .count();

        if (foreignKeys.size() != 2 || nonFkNonIdCount > 0) return false;

        Set<String> referencedEntities = foreignKeys.stream()
                .map(a -> a.referencedEntity)
                .collect(TreeSet::new, Set::add, Set::addAll);

        if (referencedEntities.size() != 2) return false;

        String pair = String.join("::", referencedEntities);
        return pureJoinPairs.contains(pair);
    }

    private String pairKey(String a, String b) {
        List<String> ends = new ArrayList<>(Arrays.asList(a, b));
        Collections.sort(ends);
        return ends.get(0) + "::" + ends.get(1);
    }

    private String convertToValidJDLRelationship(String relationship) {
        if (relationship == null) return null;
        switch (relationship) {
            case "OneToOne":
            case "OneToMany":
            case "ManyToOne":
            case "ManyToMany":
                return relationship;
            case "Composicion":
            case "Agregacion":
                return "OneToMany";
            case "Generalizacion":
                return null;
            case "Dependencia":
                return "OneToOne";
            case "Recursividad":
                return null;
            default:
                return "OneToMany";
        }
    }

    private String convertTypeToJDL(String goJsType) {
        if (goJsType == null) return "String";
        switch (goJsType.toLowerCase()) {
            case "int":
            case "integer":
                return "Integer";
            case "text":
            case "string":
            case "varchar":
                return "String";
            case "decimal":
            case "double":
            case "float":
                return "Double";
            case "boolean":
                return "Boolean";
            case "date":
                return "LocalDate";
            case "datetime":
                return "Instant";
            default:
                return "String";
        }
    }

    // -------------------- JHipster --------------------

    private Path executeJHipsterGeneration(Path workDir, Path jdlFile) throws IOException {
        try {
            Path projectDir = workDir.resolve("generated-project");
            Files.createDirectories(projectDir);
            System.out.println("Directorio del proyecto: " + projectDir);

            List<String> command;
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("windows")) {
                command = Arrays.asList(
                        "cmd", "/c", "jhipster", "import-jdl", jdlFile.toString(),
                        "--skip-client", "--skip-user-management", "--force", "--skip-install"
                );
            } else {
                command = Arrays.asList(
                        "jhipster", "import-jdl", jdlFile.toString(),
                        "--skip-client", "--skip-user-management", "--force", "--skip-install"
                );
            }

            System.out.println("Ejecutando comando: " + String.join(" ", command));
            System.out.println("En directorio: " + projectDir);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(projectDir.toFile());
            processBuilder.redirectErrorStream(true);
            Map<String, String> env = processBuilder.environment();
            System.out.println("PATH: " + env.get("PATH"));

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println("JHipster OUTPUT: " + line);
                }
            }

            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("JHipster generation timed out after 10 minutes");
            }

            int exitCode = process.exitValue();
            System.out.println("JHipster exit code: " + exitCode);
            if (exitCode != 0) {
                throw new RuntimeException("JHipster generation failed with exit code: " + exitCode +
                        "\nOutput: " + output);
            }

            try (var stream = Files.walk(projectDir)) {
                long fileCount = stream.filter(Files::isRegularFile).count();
                System.out.println("Archivos generados: " + fileCount);
            }

            return projectDir;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("JHipster generation was interrupted", e);
        }
    }

    // -------------------- Compresión / Limpieza --------------------

    private byte[] compressDirectoryToZip(Path sourceDir) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final Set<String> EXCLUDES = new HashSet<>(Arrays.asList(
                "node_modules", ".git", "target", "build", ".gradle", "dist", ".cache"
        ));

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path))
                    .filter(path -> {
                        Path rel = sourceDir.relativize(path);
                        for (Path part : rel) {
                            if (EXCLUDES.contains(part.toString())) return false;
                        }
                        return true;
                    })
                    .forEach(path -> {
                        String entryName = sourceDir.relativize(path).toString().replace('\\', '/');
                        ZipEntry zipEntry = new ZipEntry(entryName);
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

    private void deleteHeavyDirs(Path projectDir) {
        List<String> heavy = Arrays.asList("node_modules", ".git", "target", "build", ".gradle", "dist", ".cache");
        for (String name : heavy) {
            Path p = projectDir.resolve(name);
            if (Files.exists(p)) {
                try {
                    Files.walk(p)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                    System.out.println("Eliminado: " + p);
                } catch (IOException e) {
                    System.err.println("No se pudo eliminar " + p + ": " + e.getMessage());
                }
            }
        }
    }

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

    // -------------------- Postman / SQL (mejorados para M:N) --------------------

    private void generatePostmanCollection(Path workDir, Map<String, Object> diagramData) throws IOException {
        StringBuilder postman = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode rootNode = mapper.convertValue(diagramData, JsonNode.class);
            JsonNode dataNode = rootNode.get("data");
            JsonNode nodeDataArray = dataNode.get("nodeDataArray");
            JsonNode linkDataArray = dataNode.get("linkDataArray");

            Map<String, String> relationshipMap = new HashMap<>();
            Map<String, String> entityKeys = new HashMap<>();
            Set<String> associationEntities = new HashSet<>();

            if (nodeDataArray != null) {
                for (JsonNode node : nodeDataArray) {
                    Integer key = node.get("key").asInt();
                    String entityName = node.get("name").asText();
                    entityKeys.put(key.toString(), entityName);

                    // Detectar association entities
                    boolean isIntermediate = node.has("isIntermediateTable") && node.get("isIntermediateTable").asBoolean();
                    if (isIntermediate) {
                        JsonNode attributes = node.get("attributes");
                        long nonFkCount = 0;
                        if (attributes != null) {
                            for (JsonNode attr : attributes) {
                                boolean isFk = attr.has("foreignKey") && attr.get("foreignKey").asBoolean();
                                String name = attr.get("name").asText();
                                if (!isFk && !"id".equals(name)) {
                                    nonFkCount++;
                                }
                            }
                        }
                        if (nonFkCount > 0) {
                            associationEntities.add(entityName);
                        }
                    }
                }
            }

            if (linkDataArray != null) {
                for (JsonNode link : linkDataArray) {
                    String relationship = link.has("relationship") ? link.get("relationship").asText() : null;
                    boolean isManyToManyPart = link.has("isManyToManyPart") && link.get("isManyToManyPart").asBoolean();

                    if (("OneToMany".equals(relationship) || "Composicion".equals(relationship) || "Agregacion".equals(relationship)) && !isManyToManyPart) {
                        // Para relaciones OneToMany, Composicion y Agregacion normales, no para partes de ManyToMany
                        Integer fromKey = link.get("from").asInt();
                        Integer toKey = link.get("to").asInt();
                        String parentEntity = entityKeys.get(fromKey.toString());
                        String childEntity = entityKeys.get(toKey.toString());
                        relationshipMap.put(childEntity, parentEntity);
                    }
                }
            }

            postman.append("{\n");
            postman.append("  \"info\": {\"name\": \"Generated API Backend\",\"description\": \"API endpoints generados automáticamente - SIN AUTENTICACIÓN\",\"schema\": \"https://schema.getpostman.com/json/collection/v2.1.0/collection.json\"},\n");
            postman.append("  \"variable\": [ { \"key\": \"baseUrl\", \"value\": \"http://localhost:8080\" } ],\n");
            postman.append("  \"item\": [\n");

            boolean firstEntity = true;

            if (nodeDataArray != null) {
                for (JsonNode node : nodeDataArray) {
                    String entityName = node.get("name").asText();

                    // Saltear pure joins (tablas intermedias sin campos extra)
                    boolean isIntermediate = node.has("isIntermediateTable") && node.get("isIntermediateTable").asBoolean();
                    if (isIntermediate && !associationEntities.contains(entityName)) {
                        continue; // Es pure join, no generar endpoints
                    }

                    String entityLower = convertEntityNameToUrl(entityName);
                    JsonNode attributes = node.get("attributes");
                    String parentEntity = relationshipMap.get(entityName);

                    if (!firstEntity) postman.append(",\n");
                    firstEntity = false;

                    postman.append("    {\"name\":\"").append(entityName).append(" API\",\"item\":[\n");
                    postman.append("      {\"name\":\"Get All ").append(entityName).append("s\",\"request\":{\"method\":\"GET\",\"header\":[],\"url\":\"{{baseUrl}}/api/").append(entityLower).append("s\"}},\n");
                    postman.append("      {\"name\":\"Get ").append(entityName).append(" by ID\",\"request\":{\"method\":\"GET\",\"header\":[],\"url\":\"{{baseUrl}}/api/").append(entityLower).append("s/1\"}},\n");

                    // POST
                    postman.append("      {\"name\":\"Create ").append(entityName).append("\",\"request\":{\"method\":\"POST\",\"header\":[{\"key\":\"Content-Type\",\"value\":\"application/json\"}],\"body\":{\"mode\":\"raw\",\"raw\":\"{\\n");
                    boolean firstAttr = true;
                    if (attributes != null) {
                        for (JsonNode attr : attributes) {
                            String attrName = attr.get("name").asText();
                            String attrType = attr.get("type").asText();
                            boolean isForeignKey = attr.has("foreignKey") && attr.get("foreignKey").asBoolean();

                            if (!"id".equals(attrName)) {
                                if (!firstAttr) postman.append(",\\n");
                                firstAttr = false;

                                if (isForeignKey) {
                                    if (associationEntities.contains(entityName)) {
                                        // Para association entities, usar el nombre de la relación basado en referencedEntity
                                        String referencedEntity = attr.has("referencedEntity") ? attr.get("referencedEntity").asText() : "Entity";
                                        String relationName = convertFkNameToRelationName(attrName, referencedEntity);
                                        postman.append("  \\\"").append(relationName).append("\\\": {\\n    \\\"id\\\": 1\\n  }");
                                    } else if (parentEntity != null) {
                                        // Para relaciones OneToMany normales
                                        String fkName = parentEntity.toLowerCase();
                                        postman.append("  \\\"").append(fkName).append("\\\": {\\n    \\\"id\\\": 1\\n  }");
                                    }
                                } else if (!isForeignKey) {
                                    String sampleValue = getSampleValue(attrType);
                                    postman.append("  \\\"").append(attrName).append("\\\": ").append(sampleValue);
                                }
                            }
                        }
                    }
                    postman.append("\\n}\"},\"url\":\"{{baseUrl}}/api/").append(entityLower).append("s\"}},\n");

                    // PUT
                    postman.append("      {\"name\":\"Update ").append(entityName).append("\",\"request\":{\"method\":\"PUT\",\"header\":[{\"key\":\"Content-Type\",\"value\":\"application/json\"}],\"body\":{\"mode\":\"raw\",\"raw\":\"{\\n  \\\"id\\\": 1");
                    if (attributes != null) {
                        for (JsonNode attr : attributes) {
                            String attrName = attr.get("name").asText();
                            String attrType = attr.get("type").asText();
                            boolean isForeignKey = attr.has("foreignKey") && attr.get("foreignKey").asBoolean();

                            if (!"id".equals(attrName)) {
                                if (isForeignKey) {
                                    if (associationEntities.contains(entityName)) {
                                        // Para association entities
                                        String referencedEntity = attr.has("referencedEntity") ? attr.get("referencedEntity").asText() : "Entity";
                                        String relationName = convertFkNameToRelationName(attrName, referencedEntity);
                                        postman.append(",\\n  \\\"").append(relationName).append("\\\": {\\n    \\\"id\\\": 1\\n  }");
                                    } else if (parentEntity != null) {
                                        // Para relaciones OneToMany normales
                                        String fkName = parentEntity.toLowerCase();
                                        postman.append(",\\n  \\\"").append(fkName).append("\\\": {\\n    \\\"id\\\": 1\\n  }");
                                    }
                                } else if (!isForeignKey) {
                                    String sampleValue = getSampleValue(attrType);
                                    postman.append(",\\n  \\\"").append(attrName).append("\\\": ").append(sampleValue);
                                }
                            }
                        }
                    }
                    postman.append("\\n}\"},\"url\":\"{{baseUrl}}/api/").append(entityLower).append("s/1\"}},\n");

                    // DELETE
                    postman.append("      {\"name\":\"Delete ").append(entityName).append("\",\"request\":{\"method\":\"DELETE\",\"header\":[],\"url\":\"{{baseUrl}}/api/").append(entityLower).append("s/1\"}}\n");

                    postman.append("    ]}");
                }
            }

            postman.append("\n  ]\n");
            postman.append("}\n");

            Path postmanFile = workDir.resolve("API_Postman_Collection.json");
            Files.writeString(postmanFile, postman.toString());
            System.out.println("Colección de Postman generada: " + postmanFile);

        } catch (Exception e) {
            throw new RuntimeException("Error al generar colección de Postman: " + e.getMessage(), e);
        }
    }

    private void generateDataScript(Path workDir, Map<String, Object> diagramData) throws IOException {
        StringBuilder sql = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode rootNode = mapper.convertValue(diagramData, JsonNode.class);
            JsonNode dataNode = rootNode.get("data");
            JsonNode nodeDataArray = dataNode.get("nodeDataArray");
            JsonNode linkDataArray = dataNode.get("linkDataArray");

            Map<String, String> relationshipMap = new HashMap<>();
            Map<String, String> entityKeys = new HashMap<>();
            Set<String> associationEntities = new HashSet<>();
            Set<String> pureJoins = new HashSet<>();

            if (nodeDataArray != null) {
                for (JsonNode node : nodeDataArray) {
                    Integer key = node.get("key").asInt();
                    String entityName = node.get("name").asText();
                    entityKeys.put(key.toString(), entityName);

                    // Detectar tipos de entidades intermedias
                    boolean isIntermediate = node.has("isIntermediateTable") && node.get("isIntermediateTable").asBoolean();
                    if (isIntermediate) {
                        JsonNode attributes = node.get("attributes");
                        long nonFkCount = 0;
                        if (attributes != null) {
                            for (JsonNode attr : attributes) {
                                boolean isFk = attr.has("foreignKey") && attr.get("foreignKey").asBoolean();
                                String name = attr.get("name").asText();
                                if (!isFk && !"id".equals(name)) {
                                    nonFkCount++;
                                }
                            }
                        }
                        if (nonFkCount > 0) {
                            associationEntities.add(entityName);
                        } else {
                            pureJoins.add(entityName);
                        }
                    }
                }
            }

            if (linkDataArray != null) {
                for (JsonNode link : linkDataArray) {
                    String relationship = link.get("relationship").asText();
                    boolean isManyToManyPart = link.has("isManyToManyPart") && link.get("isManyToManyPart").asBoolean();

                    if (("OneToMany".equals(relationship) || "Composicion".equals(relationship) || "Agregacion".equals(relationship)) && !isManyToManyPart) {
                        // Para relaciones OneToMany, Composicion y Agregacion normales, no para partes de ManyToMany
                        Integer fromKey = link.get("from").asInt();
                        Integer toKey = link.get("to").asInt();
                        String parentEntity = entityKeys.get(fromKey.toString());
                        String childEntity = entityKeys.get(toKey.toString());
                        relationshipMap.put(childEntity, parentEntity);
                    }
                }
            }

            sql.append("-- Script para poblar la base de datos\n");
            sql.append("-- Generado automáticamente desde el diagrama\n");
            sql.append("-- IMPORTANTE: Ejecutar después de levantar la aplicación JHipster\n");
            sql.append("-- Las tablas pure join (ManyToMany) se poblan automáticamente a través de la API\n\n");

            List<String> processedEntities = new ArrayList<>();

            if (nodeDataArray != null) {
                // Procesar primero entidades padre (sin FK)
                for (JsonNode node : nodeDataArray) {
                    String entityName = node.get("name").asText();
                    if (pureJoins.contains(entityName)) {
                        continue; // Saltear pure joins
                    }
                    if (!relationshipMap.containsKey(entityName)) {
                        generateEntityData(sql, node, null, relationshipMap, associationEntities);
                        processedEntities.add(entityName);
                    }
                }

                // Luego entidades hija (con FK)
                for (JsonNode node : nodeDataArray) {
                    String entityName = node.get("name").asText();
                    if (pureJoins.contains(entityName)) {
                        continue; // Saltear pure joins
                    }
                    if (relationshipMap.containsKey(entityName) && !processedEntities.contains(entityName)) {
                        generateEntityData(sql, node, relationshipMap.get(entityName), relationshipMap, associationEntities);
                        processedEntities.add(entityName);
                    }
                }
            }

            sql.append("-- Fin del script\n");
            sql.append("-- Para relaciones ManyToMany: usar los endpoints de la API REST\n");
            sql.append("-- Ejemplo: POST /api/productos/1/pedidos con [1,2,3] en el body\n");

            Path sqlFile = workDir.resolve("sample_data.sql");
            Files.writeString(sqlFile, sql.toString());
            System.out.println("Script de datos generado: " + sqlFile);

        } catch (Exception e) {
            throw new RuntimeException("Error al generar script de datos: " + e.getMessage(), e);
        }
    }

    private void generateEntityData(StringBuilder sql, JsonNode node, String parentEntity,
                                    Map<String, String> relationshipMap, Set<String> associationEntities) {
        String entityName = node.get("name").asText();
        String tableName = entityName.toLowerCase();
        JsonNode attributes = node.get("attributes");

        sql.append("-- Datos para la tabla ").append(tableName);
        if (parentEntity != null) {
            sql.append(" (referencia a ").append(parentEntity.toLowerCase()).append(")");
        }
        if (associationEntities.contains(entityName)) {
            sql.append(" (entidad de asociación Many-to-Many con campos extra)");
        }
        sql.append("\n");

        for (int i = 1; i <= 5; i++) {
            sql.append("INSERT INTO ").append(tableName).append(" (");

            boolean firstAttr = true;
            if (attributes != null) {
                for (JsonNode attr : attributes) {
                    String attrName = attr.get("name").asText();
                    boolean isForeignKey = attr.has("foreignKey") && attr.get("foreignKey").asBoolean();

                    if (!"id".equals(attrName)) {
                        if (!firstAttr) sql.append(", ");
                        firstAttr = false;

                        if (isForeignKey) {
                            // Para association entities, usar el nombre correcto de la columna FK
                            if (associationEntities.contains(entityName)) {
                                String referencedEntity = attr.has("referencedEntity") ?
                                        attr.get("referencedEntity").asText() : "unknown";
                                sql.append(referencedEntity.toLowerCase()).append("_id");
                            } else if (parentEntity != null) {
                                sql.append(parentEntity.toLowerCase()).append("_id");
                            } else {
                                sql.append(attrName);
                            }
                        } else {
                            sql.append(attrName);
                        }
                    }
                }
            }

            sql.append(") VALUES (");

            firstAttr = true;
            if (attributes != null) {
                for (JsonNode attr : attributes) {
                    String attrName = attr.get("name").asText();
                    String attrType = attr.get("type").asText();
                    boolean isForeignKey = attr.has("foreignKey") && attr.get("foreignKey").asBoolean();

                    if (!"id".equals(attrName)) {
                        if (!firstAttr) sql.append(", ");
                        firstAttr = false;

                        if (isForeignKey) {
                            // Valores de referencia para FKs
                            int refId = (i - 1) % 5 + 1;
                            sql.append(refId);
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

    private String getSampleValue(String type) {
        if (type == null) return "\\\"Valor\\\"";
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

    // Helper method to convert entity names to URL format (DetallePedido -> detalle-pedidos)
    private String convertEntityNameToUrl(String entityName) {
        // Convert camelCase/PascalCase to kebab-case
        String result = entityName
                .replaceAll("([a-z])([A-Z])", "$1-$2") // Insert hyphens before uppercase letters
                .toLowerCase(); // Convert to lowercase

        // Smart pluralization: avoid double 's'
        if (!result.endsWith("s") && !result.endsWith("o")) {
            result += "s";
        } else if (result.endsWith("o")) {
            result += "s";  // inventario -> inventarios
        } else if (result.endsWith("s")) {
            // Already plural or ends with 's', don't add another 's'
            // inventarios stays inventarios
        }

        return result;
    }

    // Helper method to convert FK field names to JPA relation names
    private String convertFkNameToRelationName(String fkFieldName, String referencedEntity) {
        // Convert idProducto -> producto, idPedido -> pedido, etc.
        String relationName = fkFieldName.toLowerCase();

        // Remove 'id' prefix if present
        if (relationName.startsWith("id")) {
            relationName = relationName.substring(2);
        }

        // If after removing 'id' the name matches the referenced entity (case insensitive), use it
        if (relationName.equalsIgnoreCase(referencedEntity)) {
            return relationName.toLowerCase();
        }

        // Otherwise, fall back to the referenced entity name in lowercase
        return referencedEntity.toLowerCase();
    }

    private String getSampleSQLValue(String type, int index, String fieldName) {
        if (type == null) return "'" + fieldName + " " + index + "'";
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
}