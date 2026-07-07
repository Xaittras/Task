package com.example.authapi.controller;

import com.example.authapi.dto.ProcessRequest;
import com.example.authapi.dto.ProcessResponse;
import com.example.authapi.security.AuthenticatedUser;
import com.example.authapi.service.ProcessService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProcessController {

    private final ProcessService processService;

    public ProcessController(ProcessService processService) {
        this.processService = processService;
    }

    @PostMapping("/process")
    public ResponseEntity<ProcessResponse> process(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ProcessRequest request
    ) {
        ProcessResponse response = processService.process(user.id(), request.text());
        return ResponseEntity.ok(response);
    }
}
