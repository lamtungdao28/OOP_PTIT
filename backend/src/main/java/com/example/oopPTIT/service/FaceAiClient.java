package com.example.oopPTIT.service;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@Component
public class FaceAiClient {
    private final RestClient client;
    private final String token;
    private final ObjectMapper json;

    public FaceAiClient(RestClient faceAiRestClient, @Value("${app.ai.token:}") String token,
                        ObjectMapper json) {
        this.client = faceAiRestClient;
        this.token = token;
        this.json = json;
    }

    public RegistrationResult register(MultipartFile image) {
        return post("/face/register", imageParts(image), RegistrationResult.class);
    }

    public RecognitionResult recognize(MultipartFile image, List<Candidate> candidates) {
        MultiValueMap<String, Object> parts = imageParts(image);
        parts.add("candidates", json.writeValueAsString(candidates));
        return post("/face/recognize", parts, RecognitionResult.class);
    }

    private MultiValueMap<String, Object> imageParts(MultipartFile image) {
        byte[] bytes;
        try {
            bytes = image.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded image", exception);
        }
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "face-image";
            }
        };
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.parseMediaType(image.getContentType()));
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("image", new HttpEntity<>(resource, partHeaders));
        return parts;
    }

    private <T> T post(String path, MultiValueMap<String, Object> parts, Class<T> responseType) {
        if (token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI service token is not configured");
        }
        try {
            T result = client.post().uri(path)
                    .header("X-AI-Service-Token", token)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts).retrieve().body(responseType);
            if (result == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI service returned no result");
            }
            return result;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 422) {
                throw new ResponseStatusException(HttpStatus.valueOf(422), "Face image was rejected by AI service");
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI service is unavailable");
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI service is unavailable");
        }
    }

    public record RegistrationResult(String modelName, List<Float> embedding) {
    }

    public record Candidate(Long studentId, String modelName, List<Float> embedding) {
    }

    public record RecognitionResult(Long studentId, Double confidence, String modelName) {
    }
}
