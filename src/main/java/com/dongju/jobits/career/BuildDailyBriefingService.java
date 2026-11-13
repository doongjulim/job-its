package com.dongju.jobits.career;

import com.dongju.jobits.config.JobItsConfig;
import com.dongju.jobits.job.DetectNewRecruitmentService;
import com.dongju.jobits.job.MatchedRecruitment;
import com.dongju.jobits.schedule.FindUpcomingEventService;
import com.dongju.jobits.schedule.SummarizeStudyProgressService;

import java.time.LocalDate;
import java.util.List;

/** 공고·일정·공부·지원현황·스펙을 한데 모아 브리핑을 만든다. 판단 로직은 각 서비스에 있고 여기선 조립만 한다. */
public class BuildDailyBriefingService {

    static final int CLOSING_SOON_DAYS = 3;

    private final SummarizeStudyProgressService studyService = new SummarizeStudyProgressService();
    private final SummarizeApplicationService applicationService = new SummarizeApplicationService();
    private final CheckSpecStatusService specService = new CheckSpecStatusService();

    public DailyBriefing build(JobItsConfig config,
                               List<MatchedRecruitment> openPostings,
                               DetectNewRecruitmentService.Result detection,
                               LocalDate today) {
        int minScore = config.profile().job().notifyMinScore();
        FindUpcomingEventService events = new FindUpcomingEventService(config.schedule());

        List<MatchedRecruitment> closingSoon = openPostings.stream()
                .filter(m -> m.score() >= minScore)
                .filter(m -> {
                    Long d = m.recruitment().daysUntilDeadline(today);
                    return d != null && d >= 0 && d <= CLOSING_SOON_DAYS;
                })
                .toList();

        return new DailyBriefing(
                today,
                config.profile().goal(),
                detection.newOnes(),
                detection.firstRun(),
                openPostings.size(),
                closingSoon,
                events.remindersFor(today),
                events.upcoming(today, 5),
                studyService.thisWeek(config.schedule().studyPlans(), config.studyLog(), today),
                applicationService.summarize(config.applications().applications(), today),
                specService.check(config.profile().specs(), today));
    }
}
