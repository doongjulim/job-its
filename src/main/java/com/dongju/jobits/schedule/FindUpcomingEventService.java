package com.dongju.jobits.schedule;

import com.dongju.jobits.config.ScheduleConfig;
import com.dongju.jobits.config.ScheduleConfig.ScheduleEvent;
import com.dongju.jobits.support.Dday;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** 다가오는 시험·발표 일정과, 오늘 알려야 할 D-day 리마인더를 찾는다. */
public class FindUpcomingEventService {

    /** 이 날짜(D-n)에 해당하면 아침 브리핑에서 따로 강조한다. */
    static final Set<Long> REMINDER_DAYS = Set.of(30L, 7L, 3L, 1L, 0L);

    private final ScheduleConfig schedule;

    public FindUpcomingEventService(ScheduleConfig schedule) {
        this.schedule = schedule;
    }

    public List<ScheduleEvent> upcoming(LocalDate today, int limit) {
        return schedule.events().stream()
                .filter(e -> e.date() != null && !e.date().isBefore(today))
                .sorted(Comparator.comparing(ScheduleEvent::date))
                .limit(limit)
                .toList();
    }

    public List<ScheduleEvent> remindersFor(LocalDate today) {
        return schedule.events().stream()
                .filter(e -> e.date() != null && REMINDER_DAYS.contains(Dday.between(today, e.date())))
                .sorted(Comparator.comparing(ScheduleEvent::date))
                .toList();
    }
}
