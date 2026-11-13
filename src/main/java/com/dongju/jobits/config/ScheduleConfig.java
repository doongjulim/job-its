package com.dongju.jobits.config;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

import java.time.LocalDate;
import java.util.List;

/** config/schedule.yml */
public record ScheduleConfig(List<ScheduleEvent> events, List<StudyPlan> studyPlans) {

    public ScheduleConfig {
        events = events == null ? List.of() : List.copyOf(events);
        studyPlans = studyPlans == null ? List.of() : List.copyOf(studyPlans);
    }

    public static ScheduleConfig empty() {
        return new ScheduleConfig(null, null);
    }

    public enum EventKind {
        EXAM("시험"), RESULT("발표"), REGISTRATION("접수"),
        @JsonEnumDefaultValue ETC("일정");

        private final String label;

        EventKind(String label) { this.label = label; }

        public String label() { return label; }
    }

    public record ScheduleEvent(String name, LocalDate date, EventKind kind, String memo) {
        public ScheduleEvent {
            kind = kind == null ? EventKind.ETC : kind;
            memo = memo == null ? "" : memo;
        }
    }

    /** from/until 이 비어 있으면 그쪽은 열린 구간. */
    public record StudyPlan(String subject, int weeklyGoalMinutes, LocalDate from, LocalDate until) {

        public boolean isActiveOn(LocalDate day) {
            return (from == null || !day.isBefore(from)) && (until == null || !day.isAfter(until));
        }
    }
}
