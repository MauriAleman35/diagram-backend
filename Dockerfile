# Usa una imagen base de OpenJDK para Java 19
FROM openjdk:19-jdk-alpine

# Establecer el directorio de trabajo en el contenedor
WORKDIR /app

# Copia el archivo JAR de la aplicación en el contenedor
COPY target/diagramParcialBackend-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto en el que la aplicación se ejecutará
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
