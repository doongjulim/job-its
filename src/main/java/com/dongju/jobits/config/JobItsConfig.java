package com.dongju.jobits.config;

import java.util.List;

/** config/ 폴더 전체를 읽은 결과 */
public record JobItsConfig(
        Profile profile,
        ScheduleConfig schedule,
        ApplicationsConfig applications,
        List<StudyLogEntry> studyLog
) {
    public JobItsConfig {
        studyLog = studyLog == null ? List.of() : List.copyOf(studyLog);
    }
}
