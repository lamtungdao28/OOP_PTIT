package com.example.oopPTIT.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FaceAiClientTests {
    private MockMultipartFile image() {
        return new MockMultipartFile("image", "face.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }

    @Test
    void sendsMultipartImageAndSecretToFastApi() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String vector = String.join(",", Collections.nCopies(128, "0.25"));
        server.expect(once(), requestTo("http://ai.test/face/register"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("X-AI-Service-Token", "secret"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(content().string(containsString("name=\"image\"")))
                .andRespond(withSuccess("{\"modelName\":\"SFace-2021dec\",\"embedding\":[" + vector + "]}",
                        MediaType.APPLICATION_JSON));

        FaceAiClient client = new FaceAiClient(builder.build(), "secret", new ObjectMapper());
        FaceAiClient.RegistrationResult result = client.register(image());
        assertThat(result.modelName()).isEqualTo("SFace-2021dec");
        assertThat(result.embedding()).hasSize(128);
        server.verify();
    }

    @Test
    void translatesInvalidFaceFromFastApi() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://ai.test/face/register"))
                .andRespond(withStatus(HttpStatus.valueOf(422)));
        FaceAiClient client = new FaceAiClient(builder.build(), "secret", new ObjectMapper());
        assertThatThrownBy(() -> client.register(image()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(422));
        server.verify();
    }

    @Test
    void sendsCandidatesToFaceRecognitionEndpoint() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://ai.test/face/recognize"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("X-AI-Service-Token", "secret"))
                .andExpect(content().string(containsString("name=\"candidates\"")))
                .andExpect(content().string(containsString("\"studentId\":101")))
                .andRespond(withSuccess("{\"studentId\":101,\"confidence\":0.8241,\"modelName\":\"SFace-2021dec\"}",
                        MediaType.APPLICATION_JSON));
        FaceAiClient client = new FaceAiClient(builder.build(), "secret", new ObjectMapper());
        var candidate = new FaceAiClient.Candidate(101L, "SFace-2021dec", Collections.nCopies(128, 0.25f));
        var result = client.recognize(image(), java.util.List.of(candidate));
        assertThat(result.studentId()).isEqualTo(101L);
        assertThat(result.confidence()).isEqualTo(0.8241);
        server.verify();
    }
}
