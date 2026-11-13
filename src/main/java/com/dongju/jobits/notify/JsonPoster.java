package com.dongju.jobits.notify;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/** JSON POST 한 번. 테스트에서는 람다로 바꿔 끼운다. */
@FunctionalInterface
public interface JsonPoster {

    /** @return HTTP 상태 코드. 2xx 가 아니면 호출한 쪽에서 실패로 처리한다. */
    int post(String url, Map<String, String> headers, String json) throws IOException, InterruptedException;

    static JsonPoster http() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        return (url, headers, json) -> {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            headers.forEach(builder::header);
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                System.err.println("[notify] " + url.replaceAll("bot[^/]+", "bot***") + " → " + response.statusCode()
                        + " " + abbreviate(response.body()));
            }
            return response.statusCode();
        };
    }

    private static String abbreviate(String s) {
        return s == null ? "" : s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
