package com.dongju.jobits.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * API JSON 응답 → Recruitment 목록.
 *
 * 응답 형태: { "resultCode": 200, "resultMsg": "...", "totalCount": N, "result": [ {...}, ... ] }
 * 개별 필드는 path() 로 읽어서, 필드가 빠지거나 이름이 달라도 예외 대신 빈 값이 된다.
 * 첫 실호출 때 --raw 로 실제 필드명을 확인하고 아래 상수만 고치면 된다.
 */
public class ParseRecruitmentService {

    static final String F_ID = "recrutPblntSn";
    static final String F_INSTITUTION = "instNm";
    static final String F_TITLE = "recrutPbancTtl";
    static final String F_START = "pbancBgngYmd";
    static final String F_END = "pbancEndYmd";
    static final String F_NCS_NAMES = "ncsCdNmLst";
    static final String F_HIRE_TYPE_NAMES = "hireTypeNmLst";
    static final String F_REGION_NAMES = "workRgnNmLst";
    static final String F_RECRUIT_TYPE_NAME = "recrutSeNm";
    static final String F_HEADCOUNT = "recrutNope";
    static final String F_URL = "srcUrl";
    static final String F_ONGOING = "ongoingYn";

    private static final DateTimeFormatter COMPACT = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParsedPage parse(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        String resultCode = root.path("resultCode").asText("");
        if (!resultCode.isEmpty() && !"200".equals(resultCode)) {
            throw new IOException("채용정보 API 오류 (" + resultCode + "): " + root.path("resultMsg").asText());
        }
        int totalCount = root.path("totalCount").asInt(0);
        List<Recruitment> items = new ArrayList<>();
        for (JsonNode node : root.path("result")) {
            items.add(toRecruitment(node));
        }
        return new ParsedPage(totalCount, items);
    }

    private Recruitment toRecruitment(JsonNode n) {
        return new Recruitment(
                text(n, F_ID),
                text(n, F_INSTITUTION),
                text(n, F_TITLE),
                date(text(n, F_START)),
                date(text(n, F_END)),
                splitList(text(n, F_NCS_NAMES)),
                splitList(text(n, F_HIRE_TYPE_NAMES)),
                splitList(text(n, F_REGION_NAMES)),
                text(n, F_RECRUIT_TYPE_NAME),
                text(n, F_HEADCOUNT),
                text(n, F_URL),
                "Y".equalsIgnoreCase(text(n, F_ONGOING))
        );
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? "" : v.asText("").trim();
    }

    static LocalDate date(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() < 8) return null;
        try {
            return LocalDate.parse(digits.substring(0, 8), COMPACT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static List<String> splitList(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public record ParsedPage(int totalCount, List<Recruitment> items) {}
}
