package com.dongju.jobits.job;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 잡알리오 채용공고 1건.
 * 응답 필드명은 API 원본(instNm, recrutPbancTtl ...)을 그대로 쓰지 않고 읽기 쉬운 이름으로 옮겨 담는다.
 */
public record Recruitment(
        String id,
        String institutionName,
        String title,
        LocalDate applyStartDate,
        LocalDate applyEndDate,
        List<String> ncsFields,
        List<String> hireTypes,
        List<String> workRegions,
        String recruitType,
        String headcount,
        String url,
        boolean ongoing
) {

    /** 마감까지 남은 일수. 마감일 정보가 없으면 null. 오늘 마감이면 0, 지났으면 음수. */
    public Long daysUntilDeadline(LocalDate today) {
        if (applyEndDate == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(today, applyEndDate);
    }

    public String deadlineLabel(LocalDate today) {
        Long days = daysUntilDeadline(today);
        if (days == null) return "-";
        if (days < 0) return "마감";
        if (days == 0) return "D-DAY";
        return "D-" + days;
    }
}
