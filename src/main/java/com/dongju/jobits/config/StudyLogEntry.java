package com.dongju.jobits.config;

import java.time.LocalDate;

/** config/study-log.csv 의 한 줄 */
public record StudyLogEntry(LocalDate date, String subject, int minutes, String memo) {
}
