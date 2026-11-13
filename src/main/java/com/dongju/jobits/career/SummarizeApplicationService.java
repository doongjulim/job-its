package com.dongju.jobits.career;

import com.dongju.jobits.config.ApplicationsConfig.Application;
import com.dongju.jobits.support.Dday;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 지원 현황: 단계별 개수 + 7일 안에 마감되거나 일정(필기·면접)이 있는 건 */
public class SummarizeApplicationService {

    static final List<String> STAGE_ORDER = List.of("관심", "서류", "필기", "면접", "최종합격", "불합격");
    static final int SOON_DAYS = 7;

    public Summary summarize(List<Application> applications, LocalDate today) {
        Map<String, Long> byStage = new LinkedHashMap<>();
        STAGE_ORDER.forEach(stage -> byStage.put(stage, 0L));
        applications.forEach(a -> byStage.merge(a.stage(), 1L, Long::sum));
        byStage.values().removeIf(count -> count == 0);

        List<Application> soon = applications.stream()
                .filter(a -> !a.isClosed())
                .filter(a -> isSoon(a.deadline(), today) || isSoon(a.nextDate(), today))
                .sorted(Comparator.comparing(a -> nearest(a, today), Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return new Summary(byStage, soon);
    }

    private static boolean isSoon(LocalDate date, LocalDate today) {
        if (date == null) return false;
        long d = Dday.between(today, date);
        return d >= 0 && d <= SOON_DAYS;
    }

    private static LocalDate nearest(Application a, LocalDate today) {
        LocalDate deadline = isSoon(a.deadline(), today) ? a.deadline() : null;
        LocalDate next = isSoon(a.nextDate(), today) ? a.nextDate() : null;
        if (deadline == null) return next;
        if (next == null) return deadline;
        return deadline.isBefore(next) ? deadline : next;
    }

    public record Summary(Map<String, Long> countByStage, List<Application> dueSoon) {}
}
