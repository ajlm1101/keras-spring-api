package com.example.kerasapi.service;

import com.example.kerasapi.model.Prediction;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import keras.KerasGrpc;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class KerasService {

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

}
