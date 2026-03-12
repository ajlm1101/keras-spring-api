package com.example.kerasapi.model;

public class Prediction {

    private String filename;
    private String predictedClass;
    private float confidence;

    public Prediction() {}

    public Prediction(String filename, String predictedClass, float confidence) {
        this.filename = filename;
        this.predictedClass = predictedClass;
        this.confidence = confidence;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getPredictedClass() {
        return predictedClass;
    }

    public void setPredictedClass(String predictedClass) {
        this.predictedClass = predictedClass;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

}
