package com.example.authapi.client;

import com.example.authapi.dto.TransformRequest;
import com.example.authapi.dto.TransformResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Talks to Service B (data-api) over HTTP, attaching the shared internal
 * token header so data-api can trust the caller.
 */
@Component
public class DataApiClient {

    private final RestTemplate restTemplate;
    private final String dataApiUrl;
    private final String internalToken;

    public DataApiClient(
            RestTemplate restTemplate,
            @Value("${app.data-api-url}") String dataApiUrl,
            @Value("${app.internal-token}") String internalToken
    ) {
        this.restTemplate = restTemplate;
        this.dataApiUrl = dataApiUrl;
        this.internalToken = internalToken;
    }

    public String transform(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Token", internalToken);

        HttpEntity<TransformRequest> entity = new HttpEntity<>(new TransformRequest(text), headers);

        try {
            TransformResponse response = restTemplate.postForObject(
                    dataApiUrl + "/api/transform",
                    entity,
                    TransformResponse.class
            );
            if (response == null) {
                throw new DataApiException("data-api returned an empty response");
            }
            return response.result();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            HttpStatusCode status = e.getStatusCode();
            throw new DataApiException("data-api call failed with status " + status.value(), e);
        } catch (ResourceAccessException e) {
            throw new DataApiException("data-api is unreachable", e);
        }
    }

    public static class DataApiException extends RuntimeException {
        public DataApiException(String message) {
            super(message);
        }

        public DataApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
