package com.dongju.jobits;

import com.dongju.jobits.config.JobItsConfig;
import com.dongju.jobits.config.LoadConfigService;
import com.dongju.jobits.config.StudyLogEntry;
import com.dongju.jobits.job.AlioRecruitmentSource;
import com.dongju.jobits.job.DetectNewRecruitmentService;
import com.dongju.jobits.job.FetchAlioRecruitmentClient;
import com.dongju.jobits.job.FixtureRecruitmentSource;
import com.dongju.jobits.job.ParseRecruitmentService;
import com.dongju.jobits.job.RecruitmentSearchCondition;
import com.dongju.jobits.job.RecruitmentSource;
import com.dongju.jobits.notify.ConsoleNotifier;
import com.dongju.jobits.notify.JsonPoster;
import com.dongju.jobits.notify.NotificationDispatcher;
import com.dongju.jobits.state.SeenRecruitmentStore;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 잡있으(job it's) — 공기업 취업 개인 비서
 *
 * <pre>
 *   ./gradlew run                                  실제 실행 (ALIO_API_KEY 필요)
 *   ./gradlew run --args="demo"                    샘플 공고로 결과물만 만들어 보기 (키 불필요)
 *   ./gradlew run --args="raw"                     API 원본 응답 출력 (필드명 점검)
 *   ./gradlew run --args="log TOEIC 90 RC파트5"     공부 기록 추가 (config/study-log.csv)
 * </pre>
 *
 * 환경변수: ALIO_API_KEY, DISCORD_WEBHOOK_URL, TELEGRAM_BOT_TOKEN, TELEGRAM_CHAT_ID,
 *          JOBITS_ISSUES(true면 새 공고를 이슈로), JOBITS_MODE(full|jobs-only),
 *          JOBITS_NOTIFY(false면 알림 끔), JOBITS_TODAY(YYYY-MM-DD, 테스트용)
 */
public class JobItsApp {

    static final ZoneId KST = ZoneId.of("Asia/Seoul");
    static final Path CONFIG_DIR = Path.of("config");
    static final Path OUTPUT_DIR = Path.of("generated");
    static final Path STATE_FILE = Path.of("state", "seen-jobs.json");
    static final Path DEMO_FIXTURE = Path.of("fixtures", "alio-sample.json");

    public static void main(String[] args) throws Exception {
        // JDK·OS 로케일과 상관없이 로그 한글이 깨지지 않게
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
        String command = args.length == 0 ? "run" : args[0];
        Map<String, String> env = System.getenv();
        LocalDate today = env.containsKey("JOBITS_TODAY") ? LocalDate.parse(env.get("JOBITS_TODAY")) : LocalDate.now(KST);

        switch (command) {
            case "run" -> run(env, today);
            case "demo" -> demo(today);
            case "raw" -> System.out.println(new FetchAlioRecruitmentClient(env.get("ALIO_API_KEY"))
                    .fetchRawJson(RecruitmentSearchCondition.from(new LoadConfigService(CONFIG_DIR).load().profile().job())));
            case "log" -> log(Arrays.copyOfRange(args, 1, args.length), today);
            default -> {
                System.err.println("알 수 없는 명령: " + command + "  (run | demo | raw | log)");
                System.exit(2);
            }
        }
    }

    private static void run(Map<String, String> env, LocalDate today) throws Exception {
        JobItsConfig config = new LoadConfigService(CONFIG_DIR).load();
        String apiKey = env.get("ALIO_API_KEY");
        RecruitmentSource source;
        if (apiKey == null || apiKey.isBlank()) {
            // 키가 없어도 일정·공부·지원현황 카드는 만들고, 공고 수집만 건너뛴다
            System.out.println("::warning title=ALIO_API_KEY 없음::공고 수집을 건너뜁니다. "
                    + "Settings > Secrets and variables > Actions 에 ALIO_API_KEY 를 등록하세요.");
            source = condition -> List.of();
        } else {
            source = new AlioRecruitmentSource(new FetchAlioRecruitmentClient(apiKey), new ParseRecruitmentService(), 5);
        }
        NotificationDispatcher dispatcher = NotificationDispatcher.fromEnvironment(env, JsonPoster.http());
        System.out.println("[잡있으] " + today + " · 알림 채널: " + dispatcher.channelNames());

        RunDailyPipeline.Mode mode = "jobs-only".equalsIgnoreCase(env.get("JOBITS_MODE"))
                ? RunDailyPipeline.Mode.JOBS_ONLY : RunDailyPipeline.Mode.FULL;
        boolean notify = !"false".equalsIgnoreCase(env.get("JOBITS_NOTIFY"));

        RunDailyPipeline.Result result = new RunDailyPipeline(config, source, new SeenRecruitmentStore(STATE_FILE),
                dispatcher, OUTPUT_DIR).run(today, mode, notify);
        System.out.println("[잡있으] " + result.summary());
        // 알림 실패는 빌드를 깨지 않고 Actions 경고로만 남긴다
        result.failures().forEach(f -> System.out.println("::warning title=알림 실패::" + f));
    }

    /** API 키 없이 fixtures/ 샘플로 결과물을 만들어 본다. 일부 공고는 '이미 본 것'으로 처리해서 NEW 표시가 보이게 한다. */
    private static void demo(LocalDate today) throws Exception {
        JobItsConfig config = new LoadConfigService(CONFIG_DIR).load();
        ParseRecruitmentService parser = new ParseRecruitmentService();
        FixtureRecruitmentSource source = new FixtureRecruitmentSource(DEMO_FIXTURE, parser);

        Path demoState = Path.of("build", "demo", "seen-jobs.json");
        Files.deleteIfExists(demoState);
        SeenRecruitmentStore seed = new SeenRecruitmentStore(demoState).load();
        seed.addAll(source.fetchAll(null).stream().limit(3).map(DetectNewRecruitmentService::keyOf).toList());
        seed.save();

        RunDailyPipeline.Result result = new RunDailyPipeline(config, source, new SeenRecruitmentStore(demoState),
                new NotificationDispatcher(List.of(new ConsoleNotifier())), OUTPUT_DIR)
                .run(today, RunDailyPipeline.Mode.FULL, true);
        System.out.println("[잡있으 demo] " + result.summary());
        System.out.println("[잡있으 demo] 결과물: " + OUTPUT_DIR.toAbsolutePath());
    }

    private static void log(String[] args, LocalDate today) throws Exception {
        if (args.length < 2) {
            System.err.println("사용법: log <과목> <분> [메모]   예) log TOEIC 90 RC파트5");
            System.exit(2);
        }
        String memo = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "";
        StudyLogEntry entry = new StudyLogEntry(today, args[0], Integer.parseInt(args[1]), memo);
        new LoadConfigService(CONFIG_DIR).appendStudyLog(entry);
        System.out.println("기록했어요: " + entry.date() + " " + entry.subject() + " " + entry.minutes() + "분");
    }
}
