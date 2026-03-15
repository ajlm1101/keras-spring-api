# Imagen base
FROM eclipse-temurin:21-jdk

# Directorio de trabajo
WORKDIR /app

# Variables de entorno
ENV GRPC_HOST="keras-grpc"
ENV GRPC_PORT=50051

# Copiamos archivos del host al contenedor
COPY target/keras-spring-api-0.0.1-SNAPSHOT.jar app.jar

# Exponemos puerto de la API
EXPOSE 8080

# Iniciamos la API
CMD ["java", "-jar", "app.jar"]