# Spring API para Keras gRPC

## Descripción general

Este proyecto implementa una **API REST en Spring Boot** que actúa como **cliente de un servidor gRPC** para la clasificación de imágenes de flores.
La API permite enviar una imagen mediante un endpoint HTTP POST `/predict` y recibe como respuesta la clase de flor predicha junto con su nivel de confianza, utilizando un servidor gRPC que ejecuta un modelo entrenado con TensorFlow/Keras.

---

## Arquitectura del sistema

El flujo de ejecución es el siguiente:

1. Un cliente envía una imagen al endpoint `/predict` de la API REST.
2. El **controller** recibe la petición y extrae el archivo de imagen.
3. El **service** crea una petición gRPC y la envía al servidor gRPC que contiene el modelo.
4. El servidor gRPC devuelve la predicción.
5. El service mapea la respuesta gRPC a un objeto `Prediction`.
6. La API REST devuelve la predicción al cliente.

```
Cliente REST -> Controller Spring -> Service gRPC -> Servidor Keras gRPC -> Respuesta -> Controller -> Cliente
```

---

## Librerías utilizadas

* **Spring Boot 4.0.3**: framework principal para crear la API REST.
* **Spring WebMVC**: para manejo de peticiones HTTP y multipart/form-data.
* **gRPC Java 1.77.0**: permite la comunicación con el servidor gRPC de Keras.
* **Protocol Buffers 3.25.3**: serializa la imagen y los mensajes enviados/recibidos por gRPC.

El `pom.xml` ya incluye la generación automática de código gRPC a partir de `keras_grpc.proto` mediante `protobuf-maven-plugin`.

---

## Ejecución del proyecto

### Instalación de dependencias

Este proyecto requiere **Java 21** y **Maven**. Se debe realizar un **clean package** antes de iniciar la aplicación para generar el ejecutable y sus dependencias:

```bash
mvn clean package
```

### Ejecutar el servicio

Para iniciar la aplicación, desde el directorio target generado ejecutar:

```bash
java -jar keras-spring-api-0.0.1-SNAPSHOT.jar
```

La API REST estará disponible en:

```
http://localhost:8080
```

---

## Explicación del código

### Controller: `KerasController`

Expone el endpoint POST `/predict`, recibe la imagen en formato multipart y delega la predicción al service.

```java
@PostMapping("/predict")
public Prediction predict(@RequestParam("file") MultipartFile file) throws IOException {
    return kerasService.predict(file.getOriginalFilename(), file.getBytes());
}
```

### Service: `KerasService`

Se encarga de construir la petición gRPC y comunicarse con el servidor Keras.

El constructor inicializa la comunicación con el servidor gRPC.

* Lee host y port desde la configuración de Spring (definidas en `application.yml`).
* Crea un ManagedChannel para la comunicación con el servidor.
* Genera un `BlockingStub`, que se utilizará para invocar el servicio de predicción.

```java
private keras.KerasPredictionGrpc.KerasPredictionBlockingStub stub;

public KerasService(@Value("${grpc.host}") String host, @Value("${grpc.port}") int port) {
    ManagedChannel channel = ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .build();
    stub = keras.KerasPredictionGrpc.newBlockingStub(channel);
}
```

Envía una imagen al servidor Keras y devuelve la predicción:

* Construye una petición `ImageRequest` con el nombre y los bytes de la imagen.
* Invoca el método `predict` del servicio gRPC.
* Convierte la respuesta (`PredictionResponse`) en un objeto `Prediction`.

```java
public Prediction predict(String filename, byte[] fileBytes) throws IOException {
    KerasGrpc.ImageRequest request = KerasGrpc.ImageRequest.newBuilder()
        .setFilename(filename)
        .setImage(ByteString.copyFrom(fileBytes))
        .build();
    KerasGrpc.PredictionResponse response = stub.predict(request);
    return new Prediction(
        response.getFilename(),
        response.getPredictedClass(),
        response.getConfidence()
    );
}
```

### Modelo de respuesta: `Prediction`

Representa la predicción devuelta al cliente:

```java
public class Prediction {
    private String filename;
    private String predictedClass;
    private float confidence;
}
```

* `filename`: nombre del archivo recibido
* `predictedClass`: clase predicha por el modelo
* `confidence`: probabilidad de la predicción

#### Respuesta

La API devuelve un JSON con la siguiente estructura:

```json
{
  "filename": "flower.jpg",
  "predictedClass": "sunflowers",
  "confidence": 0.97
}
```

---

## Dockerización y despliegue

La API REST puede ejecutarse dentro de un **contenedor Docker** para facilitar su despliegue y asegurar que se ejecute en un entorno controlado.

### Dockerfile

El proyecto contiene un `Dockerfile` que realiza las siguientes acciones:

* Utiliza **Eclipse Temurin OpenJDK 21** como imagen base.
* Crea dos variables de entorno para la conexión con el servicio gRCP.
* Copia el jar compilado dentro del contenedor.
* Expone el puerto **8080** donde se ejecuta la API REST.
* Inicia la aplicación utilizando `java -jar app.jar`.

```Dockerfile
FROM eclipse-temurin:21-jdk
WORKDIR /app
ENV GRPC_HOST="keras-grpc"
ENV GRPC_PORT=50051
COPY target/keras-spring-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

### Construcción de la imagen

Para construir el contenedor se deberá ejecutar el siguiente comando desde la raíz del proyecto en la que se encuentra el `Dockerfile`.

```bash
docker build -t keras-spring-api:v1 .
```

### Ejecución del contenedor

Para ejecutar el contenedor deberá usarse el siguiende comando:

```bash
docker run --name keras-spring-api -p 8080:8080 keras-spring-api:v1
```

La API REST estará accesible en:

```
http://localhost:8080
```

### Docker Compose

El proyecto también incluye un fichero `docker-compose.yml` que permite ejecutar conjuntamente la API REST y el servidor gRPC del modelo Keras.

Se definen dos servicios:

* **keras-api**:

  * Construido a partir del Dockerfile del proyecto.
  * Expone el puerto 8080 para acceder a la API.

* **keras-grpc**:

  * Utiliza la imagen keras-grpc:v1, generada en el proyecto del modelo.
  * No expone puertos al host, por lo que solo es accesible desde la red interna de Docker.
  * De esta forma, el servicio gRPC no puede invocarse directamente desde la máquina anfitriona, y la API REST actúa como único punto de entrada al sistema.

```yaml
services:
  keras-api:
    build: .
    container_name: keras-api
    ports:
      - "8080:8080"
    depends_on:
      - keras-grpc
  keras-grpc:
    image: keras-grpc:v1
    container_name: keras-grpc
```

### Despliegue del Docker Compose

Para ejecutar ambos conenedores deberá usarse el siguiende comando:

```bash
docker-compose up -d
```

La API REST estará accesible en:

```
http://localhost:8080
```

---

## Posibles mejoras

* Añadir validación de tipo de imagen.
* Agregar más datos a la respuesta que pueda proporcionar el modelo.
* Implementar predicción por lotes en caso de recibir varias imágenes.
* Mejorar la seguridad del contenedor mediante grupos y usuarios.
