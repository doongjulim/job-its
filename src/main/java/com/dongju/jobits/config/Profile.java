package com.dongju.jobits.config;

import java.time.LocalDate;
import java.util.List;

/** config/profile.yml */
public record Profile(String name, String goal, JobPreference job, List<Spec> specs) {

    public Profile {
        name = name == null ? "" : name;
        goal = goal == null ? "" : goal;
        job = job == null ? JobPreference.empty() : job;
        specs = specs == null ? List.of() : List.copyOf(specs);
    }

    public record JobPreference(
            String ncsCode,
            String hireTypeCode,
            List<String> watchInstitutions,
            List<String> keywords,
            List<String> preferredRegions,
            List<String> recruitTypes,
            Integer notifyMinScore
    ) {
        public JobPreference {
            watchInstitutions = nonNull(watchInstitutions);
            keywords = nonNull(keywords);
            preferredRegions = nonNull(preferredRegions);
            recruitTypes = nonNull(recruitTypes);
            notifyMinScore = notifyMinScore == null ? 30 : notifyMinScore;
        }

        public static JobPreference empty() {
            return new JobPreference(null, null, null, null, null, null, null);
        }
    }

    /** 보유(또는 준비 중) 자격·어학. acquired 가 없으면 '준비 중'. */
    public record Spec(String name, String grade, LocalDate acquired, Integer validYears) {

        public LocalDate expiresOn() {
            if (acquired == null || validYears == null) return null;
            return acquired.plusYears(validYears);
        }
    }

    static List<String> nonNull(List<String> list) {
        return list == null ? List.of() : list.stream().filter(s -> s != null && !s.isBlank()).toList();
    }
}
