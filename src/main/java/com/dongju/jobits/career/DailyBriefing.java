package com.dongju.jobits.career;

import com.dongju.jobits.career.CheckSpecStatusService.SpecStatus;
import com.dongju.jobits.career.SummarizeApplicationService.Summary;
import com.dongju.jobits.config.ScheduleConfig.ScheduleEvent;
import com.dongju.jobits.job.MatchedRecruitment;
import com.dongju.jobits.schedule.SummarizeStudyProgressService.StudyProgress;

import java.time.LocalDate;
import java.util.List;

/** 아침 브리핑 한 장에 들어가는 모든 것 */
public record DailyBriefing(
        LocalDate today,
        String goal,
        List<MatchedRecruitment> newPostings,
        boolean firstRun,
        int openPostingCount,
        List<MatchedRecruitment> closingSoon,
        List<ScheduleEvent> reminders,
        List<ScheduleEvent> upcoming,
        List<StudyProgress> study,
        Summary applications,
        List<SpecStatus> specs
) {
    public List<SpecStatus> specAlerts() {
        return specs.stream().filter(SpecStatus::needsAttention).toList();
    }
}
