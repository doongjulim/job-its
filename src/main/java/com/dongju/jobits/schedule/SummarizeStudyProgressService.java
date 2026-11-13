package com.dongju.jobits.schedule;

import com.dongju.jobits.config.ScheduleConfig.StudyPlan;
import com.dongju.jobits.config.StudyLogEntry;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/** 이번 주(월~일) 과목별 공부 시간 vs 주간 목표 */
public class SummarizeStudyProgressService {

    public List<StudyProgress> thisWeek(List<StudyPlan> plans, List<StudyLogEntry> log, LocalDate today) {
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        return plans.stream()
                .filter(p -> p.isActiveOn(today))
                .map(p -> new StudyProgress(
                        p.subject(),
                        log.stream()
                                .filter(e -> e.subject().equalsIgnoreCase(p.subject()))
                                .filter(e -> !e.date().isBefore(monday) && !e.date().isAfter(sunday))
                                .mapToInt(StudyLogEntry::minutes)
                                .sum(),
                        p.weeklyGoalMinutes()))
                .toList();
    }

    public record StudyProgress(String subject, int doneMinutes, int goalMinutes) {

        /** 0.0 ~ 1.0 (목표 초과도 1.0) */
        public double ratio() {
            if (goalMinutes <= 0) return 0;
            return Math.min(1.0, (double) doneMinutes / goalMinutes);
        }

        public String label() {
            return hours(doneMinutes) + " / " + hours(goalMinutes);
        }

        private static String hours(int minutes) {
            int h = minutes / 60, m = minutes % 60;
            return m == 0 ? h + "h" : h + "h" + m + "m";
        }
    }
}
