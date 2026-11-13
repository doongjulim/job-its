package com.dongju.jobits.config;

import java.time.LocalDate;
import java.util.List;

/** config/applications.yml — 지원 현황 */
public record ApplicationsConfig(List<Application> applications) {

    public ApplicationsConfig {
        applications = applications == null ? List.of() : List.copyOf(applications);
    }

    public static ApplicationsConfig empty() {
        return new ApplicationsConfig(null);
    }

    public record Application(
            String institution,
            String title,
            String stage,
            LocalDate deadline,
            LocalDate nextDate,
            String memo
    ) {
        public Application {
            stage = stage == null || stage.isBlank() ? "관심" : stage.trim();
            memo = memo == null ? "" : memo;
        }

        public boolean isClosed() {
            return stage.equals("최종합격") || stage.equals("불합격");
        }
    }
}
