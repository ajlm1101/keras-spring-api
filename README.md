# Spring API para Keras gRPC

## Descripción general

Este proyecto implementa una **API REST en Spring Boot** que actúa como **cliente de un servidor gRPC** para la clasificación de imágenes de flores.

La API permite enviar una imagen mediante un endpoint HTTP POST `/predict` y recibe como respuesta la **clase de flor predicha junto con su nivel de confianza**, utilizando un servidor gRPC que ejecuta un modelo **MobileNetV2** entrenado con TensorFlow/Keras.

---

# Arquitectura del sistema

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

# Librerías utilizadas

* **Spring Boot 4.0.3**: framework principal para crear la API REST.
* **Spring WebMVC**: para manejo de peticiones HTTP y multipart/form-data.
* **gRPC Java 1.77.0**: permite la comunicación con el servidor gRPC de Keras.
* **Protocol Buffers 3.25.3**: serializa la imagen y los mensajes enviados/recibidos por gRPC.

El `pom.xml` ya incluye la generación automática de código gRPC a partir de `keras_grpc.proto` mediante `protobuf-maven-plugin`.

---

# Estructura del código

## Controller: `KerasController`

Expone el endpoint `/predict`, recibe la imagen y delega la predicción al service.

```java
@PostMapping("/predict")
public Prediction predict(@RequestParam("file") MultipartFile file) throws IOException {
    return kerasService.predict(file.getOriginalFilename(), file.getBytes());
}
```

---

## Service: `KerasService`

Se encarga de construir la petición gRPC y comunicarse con el servidor Keras.

```java id="vby7gu"
private keras.KerasPredictionGrpc.KerasPredictionBlockingStub stub;

public KerasService() {
    ManagedChannel channel = ManagedChannelBuilder
            .forAddress("localhost", 50051)
            .usePlaintext()
            .build();
    stub = keras.KerasPredictionGrpc.newBlockingStub(channel);
}

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

---

## Modelo de respuesta: `Prediction`

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

---

# Ejecución del proyecto

Este proyecto requiere **Java 21** y **Maven**.

## Construcción y ejecución

Ejecutar un **clean compile** antes de correr la aplicación:

```bash
mvn clean compile
mvn spring-boot:run
```

La API REST estará disponible en:

```
http://localhost:8080
```

---

# Endpoint de la API

## POST `/predict`

### Parámetros

* `file` (MultipartFile): archivo de imagen enviado como `multipart/form-data`.

### Respuesta

La API devuelve un JSON con la siguiente estructura:

```json
{
  "filename": "flower.jpg",
  "predictedClass": "sunflowers",
  "confidence": 0.97
}
```

---

# Dockerización y despliegue

La API REST puede ejecutarse dentro de un **contenedor Docker** para facilitar su despliegue y asegurar que se ejecute en un entorno controlado.

## Dockerfile

El proyecto contiene un `Dockerfile` similar que realiza las siguientes acciones:

* Utiliza **Eclipse Temurin OpenJDK 21** como imagen base.
* Copia el jar compilado dentro del contenedor.
* Expone el puerto **8080** donde se ejecuta la API REST.
* Inicia la aplicación utilizando `java -jar app.jar`.

---

## Construcción de la imagen

```bash
docker build -t keras-spring-api:v1 .
```

---

## Ejecución del contenedor

```bash
docker run --name keras-spring-api -p 8080:8080 keras-spring-api:v1
```

La API REST estará accesible en:

```
http://localhost:8080
```

y enviando peticiones al endpoint `/predict` funcionará de forma idéntica a la ejecución local.

---

# Posibles mejoras

* Añadir validación del tipo y tamaño de imagen.
* Manejar errores de conexión con el servidor gRPC.
* Implementar **batch prediction** enviando varias imágenes.
* Configurar la dirección del servidor gRPC mediante variables de entorno.
