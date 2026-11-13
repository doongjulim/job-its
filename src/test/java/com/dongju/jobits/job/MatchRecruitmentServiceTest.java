package com.dongju.jobits.job;

import com.dongju.jobits.Fixtures;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchRecruitmentServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);
    private final MatchRecruitmentService service = new MatchRecruitmentService(Fixtures.preference());

    @Test
    void 모든_조건이_맞으면_100점() {
        Recruitment r = Fixtures.recruitment("1", "한국전력공사", "전산직 신입", TODAY.plusDays(5));
        MatchedRecruitment m = service.match(r);
        assertEquals(100, m.score());
        assertEquals("★★★", m.stars());
        assertTrue(m.reasons().contains("관심기관"));
        assertTrue(m.reasons().contains("키워드:전산"));
    }

    @Test
    void 아무것도_안_맞으면_0점() {
        Recruitment r = Fixtures.recruitment("2", "OO재단", "사무원", TODAY.plusDays(5),
                "경력", List.of("부산"), List.of("무기계약직"));
        assertEquals(0, service.match(r).score());
    }

    @Test
    void 마감된_공고는_빼고_점수순_그다음_마감순으로_정렬한다() {
        Recruitment closed = Fixtures.recruitment("c", "한국전력공사", "전산 마감", TODAY.minusDays(1));
        Recruitment lowLate = Fixtures.recruitment("a", "OO재단", "사무원", TODAY.plusDays(20),
                "경력", List.of("부산"), List.of("정규직"));
        Recruitment highLate = Fixtures.recruitment("b", "국민연금공단", "IT 신입", TODAY.plusDays(10));
        Recruitment highSoon = Fixtures.recruitment("d", "한국전력공사", "전산 신입", TODAY.plusDays(2));

        List<MatchedRecruitment> result = service.matchAndSort(List.of(closed, lowLate, highLate, highSoon), TODAY);

        assertEquals(List.of("d", "b", "a"), result.stream().map(m -> m.recruitment().id()).toList());
    }
}
