package com.dongju.jobits.job;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParseRecruitmentServiceTest {

    private final ParseRecruitmentService parser = new ParseRecruitmentService();

    private String sample() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/sample-response.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void 응답_JSON을_공고_목록으로_바꾼다() throws IOException {
        ParseRecruitmentService.ParsedPage page = parser.parse(sample());

        assertEquals(3, page.totalCount());
        Recruitment first = page.items().get(0);
        assertEquals("한국전력공사", first.institutionName());
        assertEquals(LocalDate.of(2027, 3, 16), first.applyEndDate());
        assertEquals(List.of("전남", "서울"), first.workRegions());
        assertTrue(first.ongoing());
    }

    @Test
    void null이나_빠진_필드는_빈값으로_처리한다() throws IOException {
        Recruitment third = parser.parse(sample()).items().get(2);
        assertEquals("", third.url());
        assertFalse(third.ongoing());
    }

    @Test
    void 오류_코드면_예외를_던진다() {
        String error = "{\"resultCode\":\"30\",\"resultMsg\":\"SERVICE_KEY_IS_NOT_REGISTERED_ERROR\"}";
        IOException e = assertThrows(IOException.class, () -> parser.parse(error));
        assertTrue(e.getMessage().contains("30"));
    }

    @Test
    void 날짜는_yyyyMMdd와_yyyy_MM_dd_모두_읽는다() {
        assertEquals(LocalDate.of(2027, 3, 2), ParseRecruitmentService.date("20270302"));
        assertEquals(LocalDate.of(2027, 3, 2), ParseRecruitmentService.date("2027-03-02"));
        assertNull(ParseRecruitmentService.date(""));
        assertNull(ParseRecruitmentService.date("2027"));
    }
}
