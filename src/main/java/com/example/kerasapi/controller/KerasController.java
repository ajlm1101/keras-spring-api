package com.example.kerasapi.controller;

import com.example.kerasapi.model.Prediction;
import com.example.kerasapi.service.KerasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class KerasController {

    @Autowired
    KerasService kerasService;

    @PostMapping("/predict")
    public Prediction predict(@RequestParam("file") MultipartFile file) throws IOException {
        return kerasService.predict(file.getOriginalFilename(), file.getBytes());
    }

}
