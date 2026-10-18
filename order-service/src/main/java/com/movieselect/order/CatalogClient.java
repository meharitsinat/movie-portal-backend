package com.movieselect.order;

import java.util.UUID;
import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
class CatalogClient {
    private final RestClient restClient;

    CatalogClient(RestClient.Builder builder,
                  @Value("${services.movie.base-url}") String movieServiceUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = builder
                .requestFactory(requestFactory)
                .baseUrl(movieServiceUrl)
                .build();
    }

    void requireActiveMovie(UUID movieId, String accessToken) {
        try {
            restClient.get()
                    .uri("/api/movies/{id}", movieId)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound exception) {
            throw new IllegalArgumentException("Movie is not available: " + movieId);
        }
    }
}
