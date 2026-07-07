package com.example.dataapi.controller;

import com.example.dataapi.dto.TransformRequest;
import com.example.dataapi.dto.TransformResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TransformController {

    @PostMapping("/transform")
    public ResponseEntity<TransformResponse> transform(@Valid @RequestBody TransformRequest request) {
        String transformed = new StringBuilder(request.text().trim())
                .reverse()
                .toString()
                .toUpperCase();

        return ResponseEntity.ok(new TransformResponse(transformed));
    }
}
