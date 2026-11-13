package com.dongju.jobits.job;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 공공데이터포털 '재정경제부(구 기획재정부)_공공기관 채용정보 조회서비스' 호출.
 * https://www.data.go.kr/data/15125273/openapi.do
 *
 * serviceKey 는 마이페이지에 보이는 '일반 인증키(Decoding)'를 넣는다. 여기서 URL 인코딩한다.
 */
public class FetchAlioRecruitmentClient {

    public static final String BASE_URL = "https://apis.data.go.kr/1051000/recruitment/list";

    private final HttpClient httpClient;
    private final String serviceKey;
    private final String baseUrl;

    public FetchAlioRecruitmentClient(String serviceKey) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), serviceKey, BASE_URL);
    }

    FetchAlioRecruitmentClient(HttpClient httpClient, String serviceKey, String baseUrl) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalArgumentException(
                    "서비스키가 없습니다. 환경변수 ALIO_API_KEY 에 data.go.kr 일반 인증키(Decoding)를 넣어주세요.");
        }
        this.httpClient = httpClient;
        this.serviceKey = serviceKey;
        this.baseUrl = baseUrl;
    }

    public String fetchRawJson(RecruitmentSearchCondition condition) throws IOException, InterruptedException {
        URI uri = buildUri(condition);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("채용정보 API HTTP " + response.statusCode() + ": " + abbreviate(response.body()));
        }
        return response.body();
    }

    URI buildUri(RecruitmentSearchCondition condition) {
        Map<String, String> params = condition.toQueryParams();
        String query = "serviceKey=" + encode(serviceKey) + "&" + params.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
        return URI.create(baseUrl + "?" + query);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String abbreviate(String body) {
        if (body == null) return "";
        return body.length() > 300 ? body.substring(0, 300) + "..." : body;
    }
}
