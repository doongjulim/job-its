package com.dongju.jobits.job;

import java.util.List;

/** 공고 + 내 프로필 기준 맞춤 점수(0~100)와 그 이유 */
public record MatchedRecruitment(Recruitment recruitment, int score, List<String> reasons) {

    public MatchedRecruitment {
        reasons = List.copyOf(reasons);
    }

    public String stars() {
        if (score >= 70) return "★★★";
        if (score >= 40) return "★★☆";
        if (score >= 20) return "★☆☆";
        return "☆☆☆";
    }
}
