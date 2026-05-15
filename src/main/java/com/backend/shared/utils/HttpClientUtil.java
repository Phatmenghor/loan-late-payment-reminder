package com.backend.shared.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class HttpClientUtil {

    private final RestClient restClient;

    public String postForString(String url, String body, String contentType) {
        try {
            String contentTypeWithCharset = contentType;
            if (!contentType.contains("charset")) {
                contentTypeWithCharset = contentType + "; charset=UTF-8";
            }
            return restClient.post()
                    .uri(url)
                    .contentType(MediaType.valueOf(contentTypeWithCharset))
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("HTTP POST failed for URL: {}", url, e);
            throw new RuntimeException("HTTP POST request failed", e);
        }
    }

    public String postJson(String url, String jsonBody) {
        return postForString(url, jsonBody, MediaType.APPLICATION_JSON_VALUE);
    }
}
