package cams.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Component
public class EmbeddingClient {

    private final RestClient restClient;

    public EmbeddingClient(@Value("${application.embedding-service.base-url}") String baseUrl) {
        // Pin HTTP/1.1: the JDK client's default h2c upgrade attempt is rejected
        // by uvicorn's h11, dropping the body so FastAPI 422s on a missing body.
        HttpClient jdkClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(jdkClient))
                .build();
    }

    public float[] embed(String text) {
        EmbedResponse response = restClient.post()
                .uri("/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EmbedRequest(text))
                .retrieve()
                .body(EmbedResponse.class);

        return response.embedding();
    }

    private record EmbedRequest(String text) {}

    private record EmbedResponse(float[] embedding) {}
}
