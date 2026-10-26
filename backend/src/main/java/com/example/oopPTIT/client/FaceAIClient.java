package com.example.oopPTIT.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.*;
import org.springframework.web.client.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@Component
public class FaceAIClient {

    private final RestClient client;
    private final String serviceKey;

    public FaceAIClient(
            RestClient.Builder builder,
            @Value("${FACE_AI_URL:http://127.0.0.1:8001}")
            String faceAIUrl,
            @Value("${FACE_SERVICE_API_KEY:}")
            String serviceKey
    ) {
        this.client = builder
                .baseUrl(faceAIUrl)
                .build();

        this.serviceKey = serviceKey;
    }

    public EmbeddingResponse extract(
            MultipartFile image
    ) {

        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Image is required"
            );
        }

        if (image.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Image exceeds 5MB"
            );
        }

        String contentType = image.getContentType();

        if (!MediaType.IMAGE_JPEG_VALUE.equals(contentType)
                && !MediaType.IMAGE_PNG_VALUE.equals(contentType)) {

            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only JPG and PNG are supported"
            );
        }

        if (serviceKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Face service key not configured"
            );
        }

        try {

            byte[] bytes = image.getBytes();

            ByteArrayResource resource =
                    new ByteArrayResource(bytes) {

                        @Override
                        public String getFilename() {
                            return "face.jpg";
                        }
                    };

            HttpHeaders fileHeaders = new HttpHeaders();

            fileHeaders.setContentType(
                    MediaType.parseMediaType(contentType)
            );

            MultiValueMap<String, Object> body =
                    new LinkedMultiValueMap<>();

            body.add(
                    "image",
                    new HttpEntity<>(
                            resource,
                            fileHeaders
                    )
            );

            EmbeddingResponse result = client
                    .post()
                    .uri("/extract")
                    .header(
                            "X-Service-Key",
                            serviceKey
                    )
                    .contentType(
                            MediaType.MULTIPART_FORM_DATA
                    )
                    .body(body)
                    .retrieve()
                    .body(EmbeddingResponse.class);

            if (result == null
                    || result.modelName() == null
                    || result.embedding() == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Invalid AI response"
                );
            }

            return result;

        } catch (IOException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot read image",
                    e
            );

        } catch (RestClientResponseException e) {

            throw new ResponseStatusException(
                    e.getStatusCode().value() == 422
                            ? HttpStatus.UNPROCESSABLE_ENTITY
                            : HttpStatus.BAD_GATEWAY,
                    "Face AI rejected the request",
                    e
            );

        } catch (RestClientException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cannot connect to Face AI",
                    e
            );
        }
    }

    public record EmbeddingResponse(
            String modelName,
            List<Double> embedding
    ) {}
}