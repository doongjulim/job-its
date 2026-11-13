package com.dongju.jobits;

import com.dongju.jobits.config.ApplicationsConfig;
import com.dongju.jobits.config.ApplicationsConfig.Application;
import com.dongju.jobits.config.JobItsConfig;
import com.dongju.jobits.config.ScheduleConfig;
import com.dongju.jobits.config.ScheduleConfig.EventKind;
import com.dongju.jobits.config.ScheduleConfig.ScheduleEvent;
import com.dongju.jobits.config.ScheduleConfig.StudyPlan;
import com.dongju.jobits.job.Recruitment;
import com.dongju.jobits.job.RecruitmentSource;
import com.dongju.jobits.notify.NotificationDispatcher;
import com.dongju.jobits.notify.NotificationMessage;
import com.dongju.jobits.notify.Notifier;
import com.dongju.jobits.state.SeenRecruitmentStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RunDailyPipelineTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    @TempDir
    Path dir;

    private final List<NotificationMessage> sent = new ArrayList<>();
    private final List<Recruitment> postings = new ArrayList<>();

    private RunDailyPipeline pipeline() {
        ScheduleConfig schedule = new ScheduleConfig(
                List.of(new ScheduleEvent("한능검 <심화>", TODAY.plusDays(15), EventKind.EXAM, "10:00 입실")),
                List.of(new StudyPlan("한국사", 900, null, null)));
        ApplicationsConfig apps = new ApplicationsConfig(
                List.of(new Application("A공사", "전산", "서류", TODAY.plusDays(2), null, null)));
        JobItsConfig config = Fixtures.config(schedule, apps);
        RecruitmentSource source = condition -> List.copyOf(postings);
        Notifier capture = new Notifier() {
            public String name() { return "capture"; }
            public boolean supports(NotificationMessage.Kind kind) { return true; }
            public void send(NotificationMessage m) { sent.add(m); }
        };
        return new RunDailyPipeline(config, source, new SeenRecruitmentStore(dir.resolve("state/seen.json")),
                new NotificationDispatcher(List.of(capture)), dir.resolve("generated"));
    }

    @Test
    void 첫_실행은_브리핑만_두번째_실행부터_새_공고를_개별_알림() throws Exception {
        postings.add(Fixtures.recruitment("1", "한국전력공사", "전산 신입 & 경력", TODAY.plusDays(2)));
        postings.add(Fixtures.recruitment("2", "OO재단", "사무원", TODAY.plusDays(9), "경력", List.of("부산"), List.of("계약직")));

        RunDailyPipeline.Result first = pipeline().run(TODAY, RunDailyPipeline.Mode.FULL, true);
        assertTrue(first.firstRun());
        assertEquals(1, sent.size());
        assertEquals(NotificationMessage.Kind.BRIEFING, sent.get(0).kind());
        assertTrue(sent.get(0).body().contains("첫 실행"));

        sent.clear();
        postings.add(Fixtures.recruitment("3", "국민연금공단", "IT 신입", TODAY.plusDays(20)));
        postings.add(Fixtures.recruitment("4", "OO재단", "사무원2", TODAY.plusDays(9), "경력", List.of("부산"), List.of("계약직")));

        RunDailyPipeline.Result second = pipeline().run(TODAY, RunDailyPipeline.Mode.JOBS_ONLY, true);
        assertFalse(second.firstRun());
        assertEquals(2, second.newOnes());
        // 점수 30 미만(OO재단)은 개별 알림 없음, jobs-only 라 브리핑도 없음
        assertEquals(1, sent.size());
        assertEquals(NotificationMessage.Kind.POSTING, sent.get(0).kind());
        assertTrue(sent.get(0).title().contains("국민연금공단"));
    }

    @Test
    void 결과물_파일을_만들고_SVG는_올바른_XML이다() throws Exception {
        postings.add(Fixtures.recruitment("1", "한국전력공사", "전산 <신입> & \"경력\"", TODAY.plusDays(2)));

        pipeline().run(TODAY, RunDailyPipeline.Mode.FULL, false);

        Path out = dir.resolve("generated");
        for (String f : List.of("jobs.svg", "schedule.svg", "jobs.md", "briefing.md", "README.md")) {
            assertTrue(Files.exists(out.resolve(f)), f);
        }
        assertTrue(sent.isEmpty(), "notify=false 면 아무것도 안 보낸다");
        assertParsesAsXml(Files.readString(out.resolve("jobs.svg")));
        assertParsesAsXml(Files.readString(out.resolve("schedule.svg")));

        String briefing = Files.readString(out.resolve("briefing.md"));
        assertTrue(briefing.contains("## 마감 임박 공고"));
        assertTrue(briefing.contains("D-15 한능검 <심화>"));
        assertTrue(briefing.contains("A공사 [서류] 마감 D-2"));
        assertTrue(Files.readString(out.resolve("jobs.md")).contains("D-2"));
        assertTrue(Files.exists(dir.resolve("state/seen.json")));
    }

    @Test
    void 공고가_하나도_없어도_카드는_그려진다() throws Exception {
        pipeline().run(TODAY, RunDailyPipeline.Mode.FULL, false);
        String svg = Files.readString(dir.resolve("generated/jobs.svg"));
        assertParsesAsXml(svg);
        assertTrue(svg.contains("공고가 없어요"));
    }

    private static void assertParsesAsXml(String svg) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
        assertEquals("svg", doc.getDocumentElement().getNodeName());
    }
}
