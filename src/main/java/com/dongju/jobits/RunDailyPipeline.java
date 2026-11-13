package com.dongju.jobits;

import com.dongju.jobits.career.BuildDailyBriefingService;
import com.dongju.jobits.career.DailyBriefing;
import com.dongju.jobits.config.JobItsConfig;
import com.dongju.jobits.job.DetectNewRecruitmentService;
import com.dongju.jobits.job.MatchRecruitmentService;
import com.dongju.jobits.job.MatchedRecruitment;
import com.dongju.jobits.job.Recruitment;
import com.dongju.jobits.job.RecruitmentSearchCondition;
import com.dongju.jobits.job.RecruitmentSource;
import com.dongju.jobits.notify.NotificationDispatcher;
import com.dongju.jobits.notify.NotificationMessage;
import com.dongju.jobits.render.ComposeMessageService;
import com.dongju.jobits.render.RenderBoardService;
import com.dongju.jobits.render.RenderJobsCardService;
import com.dongju.jobits.render.RenderScheduleCardService;
import com.dongju.jobits.state.SeenRecruitmentStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 하루 한 번(또는 두 번) 도는 전체 흐름.
 *
 * 1. 공고 수집 → 2. 맞춤 점수 → 3. 새 공고 판별 → 4. 브리핑 조립
 * → 5. 알림 발송 → 6. SVG 카드·게시판·브리핑 파일 생성 → 7. 상태 저장
 */
public class RunDailyPipeline {

    public enum Mode {
        /** 아침: 새 공고 + 브리핑 */
        FULL,
        /** 저녁: 새 공고만 */
        JOBS_ONLY
    }

    private final JobItsConfig config;
    private final RecruitmentSource source;
    private final SeenRecruitmentStore store;
    private final NotificationDispatcher dispatcher;
    private final Path outputDir;

    private final ComposeMessageService composer = new ComposeMessageService();
    private final BuildDailyBriefingService briefingService = new BuildDailyBriefingService();
    private final RenderJobsCardService jobsCard = new RenderJobsCardService();
    private final RenderScheduleCardService scheduleCard = new RenderScheduleCardService();
    private final RenderBoardService board = new RenderBoardService();

    public RunDailyPipeline(JobItsConfig config, RecruitmentSource source, SeenRecruitmentStore store,
                            NotificationDispatcher dispatcher, Path outputDir) {
        this.config = config;
        this.source = source;
        this.store = store;
        this.dispatcher = dispatcher;
        this.outputDir = outputDir;
    }

    public Result run(LocalDate today, Mode mode, boolean notify) throws IOException, InterruptedException {
        // 1~2. 수집 + 맞춤
        RecruitmentSearchCondition condition = RecruitmentSearchCondition.from(config.profile().job());
        List<Recruitment> fetched = source.fetchAll(condition);
        List<MatchedRecruitment> open = new MatchRecruitmentService(config.profile().job()).matchAndSort(fetched, today);

        // 3. 새 공고
        store.load();
        DetectNewRecruitmentService.Result detection = new DetectNewRecruitmentService(store).detect(open);

        // 4. 브리핑
        DailyBriefing briefing = briefingService.build(config, open, detection, today);

        // 5. 알림
        List<NotificationMessage> messages = new ArrayList<>();
        if (!detection.firstRun()) {
            messages.addAll(composer.postings(detection.newOnes(), config.profile().job().notifyMinScore(), today));
        }
        if (mode == Mode.FULL) {
            messages.add(composer.briefingMessage(briefing));
        }
        NotificationDispatcher.Report report = notify
                ? dispatcher.dispatch(messages)
                : new NotificationDispatcher.Report(0, List.of());

        // 6. 결과물
        Files.createDirectories(outputDir);
        write("jobs.svg", jobsCard.render(open, detection.newOnes(), today));
        write("schedule.svg", scheduleCard.render(config.profile().goal(), briefing.upcoming(), briefing.study(), today));
        write("jobs.md", board.board(open, detection.newOnes(), today));
        write("briefing.md", composer.briefingMarkdown(briefing));
        write("README.md", board.dashboard(today));

        // 7. 상태 — 알림 이후에 저장해서, 중간에 죽으면 다음 실행에서 다시 알린다
        store.save();

        return new Result(fetched.size(), open.size(), detection.newOnes().size(), detection.firstRun(),
                messages.size(), report.sent(), report.failures());
    }

    private void write(String name, String content) throws IOException {
        Files.writeString(outputDir.resolve(name), content, StandardCharsets.UTF_8);
    }

    public record Result(int fetched, int open, int newOnes, boolean firstRun,
                         int messages, int deliveries, List<String> failures) {

        public String summary() {
            return "수집 " + fetched + "건 → 접수 중 " + open + "건, 새 공고 " + newOnes + "건"
                    + (firstRun ? " (첫 실행)" : "") + " · 메시지 " + messages + "개 / 전송 " + deliveries + "회"
                    + (failures.isEmpty() ? "" : " · 실패 " + failures.size() + "회");
        }
    }
}
