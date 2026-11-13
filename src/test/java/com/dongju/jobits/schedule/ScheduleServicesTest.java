package com.dongju.jobits.schedule;

import com.dongju.jobits.config.ScheduleConfig;
import com.dongju.jobits.config.ScheduleConfig.EventKind;
import com.dongju.jobits.config.ScheduleConfig.ScheduleEvent;
import com.dongju.jobits.config.ScheduleConfig.StudyPlan;
import com.dongju.jobits.config.StudyLogEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleServicesTest {

    private static final LocalDate FRI = LocalDate.of(2026, 11, 13);   // 금요일

    @Test
    void 지난_일정은_빼고_가까운_순으로_limit개() {
        ScheduleConfig schedule = new ScheduleConfig(List.of(
                new ScheduleEvent("지난 것", FRI.minusDays(1), EventKind.EXAM, null),
                new ScheduleEvent("토익", FRI.plusDays(44), EventKind.EXAM, null),
                new ScheduleEvent("한능검", FRI.plusDays(15), EventKind.EXAM, null),
                new ScheduleEvent("발표", FRI.plusDays(28), EventKind.RESULT, null)), null);

        List<ScheduleEvent> upcoming = new FindUpcomingEventService(schedule).upcoming(FRI, 2);

        assertEquals(List.of("한능검", "발표"), upcoming.stream().map(ScheduleEvent::name).toList());
    }

    @Test
    void D_30_7_3_1_DAY에만_리마인더() {
        ScheduleConfig schedule = new ScheduleConfig(List.of(
                new ScheduleEvent("D7", FRI.plusDays(7), EventKind.EXAM, null),
                new ScheduleEvent("D8", FRI.plusDays(8), EventKind.EXAM, null),
                new ScheduleEvent("D0", FRI, EventKind.RESULT, null),
                new ScheduleEvent("D30", FRI.plusDays(30), EventKind.REGISTRATION, null)), null);

        List<String> names = new FindUpcomingEventService(schedule).remindersFor(FRI)
                .stream().map(ScheduleEvent::name).toList();

        assertEquals(List.of("D0", "D7", "D30"), names);
    }

    @Test
    void 이번_주_월요일부터_일요일까지만_합산하고_활성_계획만_보여준다() {
        List<StudyPlan> plans = List.of(
                new StudyPlan("TOEIC", 600, null, null),
                new StudyPlan("한국사", 900, null, FRI.minusDays(1)));       // 어제 끝난 계획
        List<StudyLogEntry> log = List.of(
                new StudyLogEntry(LocalDate.of(2026, 11, 9), "TOEIC", 90, ""),   // 월
                new StudyLogEntry(LocalDate.of(2026, 11, 15), "toeic", 60, ""),  // 일 (대소문자 무시)
                new StudyLogEntry(LocalDate.of(2026, 11, 8), "TOEIC", 300, ""),  // 지난주 일
                new StudyLogEntry(LocalDate.of(2026, 11, 10), "NCS", 120, ""));

        List<SummarizeStudyProgressService.StudyProgress> result =
                new SummarizeStudyProgressService().thisWeek(plans, log, FRI);

        assertEquals(1, result.size());
        assertEquals(150, result.get(0).doneMinutes());
        assertEquals(0.25, result.get(0).ratio(), 1e-9);
        assertEquals("2h30m / 10h", result.get(0).label());
    }

    @Test
    void 목표_초과해도_진행률은_100퍼센트() {
        var p = new SummarizeStudyProgressService.StudyProgress("NCS", 500, 300);
        assertEquals(1.0, p.ratio());
    }
}
