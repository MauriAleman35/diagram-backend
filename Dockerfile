# Usar Amazon Corretto 17 como base
FROM amazoncorretto:17

# Establecer el directorio de trabajo en /app
WORKDIR /app

# Copiar el archivo JAR generado por Spring Boot al contenedor
COPY target/diagramParcialBackend-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto en el que correrá la aplicación Spring Boot
EXPOSE 3000

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
