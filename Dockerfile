FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copiar el jar compilado desde target
COPY target/keras-spring-api-0.0.1-SNAPSHOT.jar app.jar

# Exponer puerto
EXPOSE 8080

# Ejecutar la API
CMD ["java", "-jar", "app.jar"]