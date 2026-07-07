package com.example.authapi.service;

import com.example.authapi.client.DataApiClient;
import com.example.authapi.dto.ProcessResponse;
import com.example.authapi.entity.ProcessingLog;
import com.example.authapi.repository.ProcessingLogRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProcessService {

    private final DataApiClient dataApiClient;
    private final ProcessingLogRepository processingLogRepository;

    public ProcessService(DataApiClient dataApiClient, ProcessingLogRepository processingLogRepository) {
        this.dataApiClient = dataApiClient;
        this.processingLogRepository = processingLogRepository;
    }

    public ProcessResponse process(UUID userId, String inputText) {
        String outputText = dataApiClient.transform(inputText);

        ProcessingLog log = ProcessingLog.builder()
                .userId(userId)
                .inputText(inputText)
                .outputText(outputText)
                .build();

        processingLogRepository.save(log);

        return new ProcessResponse(outputText);
    }
}
