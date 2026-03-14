# Imagen base
FROM eclipse-temurin:21-jdk

# Directorio de trabajo
WORKDIR /app

# Copiamos archivos del host al contenedor
COPY target/keras-spring-api-0.0.1-SNAPSHOT.jar app.jar

# Exponemos puerto de la API
EXPOSE 8080

# Iniciamos la API
CMD ["java", "-jar", "app.jar"]