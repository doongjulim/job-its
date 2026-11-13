package com.dongju.jobits.career;

import com.dongju.jobits.config.ApplicationsConfig.Application;
import com.dongju.jobits.config.Profile.Spec;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.dongju.jobits.career.CheckSpecStatusService.State.*;
import static org.junit.jupiter.api.Assertions.*;

class CareerServicesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);
    private final CheckSpecStatusService specs = new CheckSpecStatusService();

    @Test
    void 스펙_상태_판정() {
        assertEquals(PREPARING, specs.statusOf(new Spec("정처기", null, null, null), TODAY).state());
        assertEquals(VALID, specs.statusOf(new Spec("한능검", "1급", TODAY.minusYears(1), null), TODAY).state());
        // 토익 2년: 2024-12-01 취득 → 2026-12-01 만료, 18일 남음
        CheckSpecStatusService.SpecStatus toeic = specs.statusOf(new Spec("TOEIC", "850", LocalDate.of(2024, 12, 1), 2), TODAY);
        assertEquals(EXPIRING, toeic.state());
        assertEquals(LocalDate.of(2026, 12, 1), toeic.expiresOn());
        assertTrue(toeic.needsAttention());
        assertEquals(EXPIRED, specs.statusOf(new Spec("TOEIC", "800", LocalDate.of(2024, 1, 1), 2), TODAY).state());
    }

    @Test
    void 지원현황_단계별_개수와_7일_안_일정() {
        List<Application> apps = List.of(
                new Application("A공사", "전산", "서류", TODAY.plusDays(3), null, null),
                new Application("B공사", "전산", "필기", null, TODAY.plusDays(6), null),
                new Application("C공사", "전산", "서류", TODAY.plusDays(20), null, null),
                new Application("D공사", "전산", "불합격", TODAY.plusDays(1), null, null),
                new Application("E공사", "전산", null, null, null, null));

        SummarizeApplicationService.Summary summary = new SummarizeApplicationService().summarize(apps, TODAY);

        assertEquals(List.of("관심", "서류", "필기", "불합격"), List.copyOf(summary.countByStage().keySet()));
        assertEquals(2L, summary.countByStage().get("서류"));
        assertEquals(List.of("A공사", "B공사"), summary.dueSoon().stream().map(Application::institution).toList());
    }
}
