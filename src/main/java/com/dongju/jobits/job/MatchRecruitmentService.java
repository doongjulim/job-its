package com.dongju.jobits.job;

import com.dongju.jobits.config.Profile.JobPreference;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 공고마다 맞춤 점수를 매기고, 마감 지난 공고는 빼고, 점수 높은 순 → 마감 임박 순으로 정렬한다.
 *
 * 관심 기관 +40 · 제목 키워드 +20 · 선호 근무지 +15 · 신입/경력 구분 일치 +15 · 정규직 +10 (최대 100)
 */
public class MatchRecruitmentService {

    static final int WATCHED_INSTITUTION = 40;
    static final int TITLE_KEYWORD = 20;
    static final int PREFERRED_REGION = 15;
    static final int RECRUIT_TYPE = 15;
    static final int REGULAR_JOB = 10;

    private final JobPreference preference;

    public MatchRecruitmentService(JobPreference preference) {
        this.preference = preference;
    }

    public List<MatchedRecruitment> matchAndSort(List<Recruitment> recruitments, LocalDate today) {
        return recruitments.stream()
                .filter(r -> r.daysUntilDeadline(today) == null || r.daysUntilDeadline(today) >= 0)
                .map(this::match)
                .sorted(Comparator.comparingInt(MatchedRecruitment::score).reversed()
                        .thenComparing(m -> m.recruitment().applyEndDate(),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public MatchedRecruitment match(Recruitment r) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (containsAny(r.institutionName(), preference.watchInstitutions()).isPresent()) {
            score += WATCHED_INSTITUTION;
            reasons.add("관심기관");
        }
        Optional<String> keyword = containsAny(r.title(), preference.keywords());
        if (keyword.isPresent()) {
            score += TITLE_KEYWORD;
            reasons.add("키워드:" + keyword.get());
        }
        if (r.workRegions().stream().anyMatch(region -> containsAny(region, preference.preferredRegions()).isPresent())) {
            score += PREFERRED_REGION;
            reasons.add("선호지역");
        }
        if (containsAny(r.recruitType(), preference.recruitTypes()).isPresent()) {
            score += RECRUIT_TYPE;
            reasons.add(r.recruitType());
        }
        if (r.hireTypes().stream().anyMatch(t -> t.contains("정규직"))) {
            score += REGULAR_JOB;
            reasons.add("정규직");
        }
        return new MatchedRecruitment(r, Math.min(score, 100), reasons);
    }

    private static Optional<String> containsAny(String text, List<String> candidates) {
        if (text == null || text.isBlank()) return Optional.empty();
        return candidates.stream().filter(text::contains).findFirst();
    }
}
