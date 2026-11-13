package com.dongju.jobits.render;

import com.dongju.jobits.career.CheckSpecStatusService.SpecStatus;
import com.dongju.jobits.career.DailyBriefing;
import com.dongju.jobits.config.ApplicationsConfig.Application;
import com.dongju.jobits.config.ScheduleConfig.ScheduleEvent;
import com.dongju.jobits.job.MatchedRecruitment;
import com.dongju.jobits.job.Recruitment;
import com.dongju.jobits.notify.NotificationMessage;
import com.dongju.jobits.notify.NotificationMessage.Kind;
import com.dongju.jobits.schedule.SummarizeStudyProgressService.StudyProgress;
import com.dongju.jobits.support.Dday;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.dongju.jobits.support.Texts.ellipsis;
import static com.dongju.jobits.support.Texts.orDash;

/** 공고·브리핑을 메신저 메시지와 마크다운 문서로 만든다. */
public class ComposeMessageService {

    /** 한 번에 개별 알림으로 보낼 새 공고 최대 수. 넘치면 브리핑/게시판에서 확인. */
    public static final int MAX_POSTING_MESSAGES = 10;

    public List<NotificationMessage> postings(List<MatchedRecruitment> newOnes, int minScore, LocalDate today) {
        return newOnes.stream()
                .filter(m -> m.score() >= minScore)
                .limit(MAX_POSTING_MESSAGES)
                .map(m -> posting(m, today))
                .toList();
    }

    NotificationMessage posting(MatchedRecruitment m, LocalDate today) {
        Recruitment r = m.recruitment();
        String title = "[새 공고] " + m.stars() + " " + r.institutionName() + " — " + ellipsis(r.title(), 40);
        String body = String.join("\n",
                r.title(),
                "접수 " + date(r.applyStartDate()) + " ~ " + date(r.applyEndDate())
                        + " (" + Dday.label(today, r.applyEndDate()) + ")",
                orDash(r.recruitType()) + " · " + (r.workRegions().isEmpty() ? "-" : String.join(", ", r.workRegions()))
                        + " · " + orDash(r.headcount()) + "명",
                "맞춤 " + m.score() + "점: " + (m.reasons().isEmpty() ? "-" : String.join(", ", m.reasons())));
        return new NotificationMessage(Kind.POSTING, title, body, r.url());
    }

    public NotificationMessage briefingMessage(DailyBriefing b) {
        String title = "[잡있으] " + dateLabel(b.today()) + " 브리핑";
        return new NotificationMessage(Kind.BRIEFING, title, String.join("\n\n", sections(b, false)), "");
    }

    public String briefingMarkdown(DailyBriefing b) {
        return "# 잡있으 · " + dateLabel(b.today()) + " 브리핑\n\n"
                + String.join("\n\n", sections(b, true)) + "\n";
    }

    private List<String> sections(DailyBriefing b, boolean markdown) {
        List<String> out = new ArrayList<>();
        if (!b.goal().isBlank()) out.add((markdown ? "> " : "") + "목표: " + b.goal());

        if (b.firstRun()) {
            out.add(section(markdown, "첫 실행", List.of(
                    "접수 중 공고 " + b.openPostingCount() + "건을 수집했어요. 다음 실행부터 새 공고만 알려드려요.")));
        } else {
            List<String> lines = b.newPostings().stream().limit(8)
                    .map(m -> m.stars() + " " + m.recruitment().institutionName() + " — "
                            + ellipsis(m.recruitment().title(), 30) + " (" + Dday.label(b.today(), m.recruitment().applyEndDate()) + ")")
                    .collect(Collectors.toCollection(ArrayList::new));
            if (b.newPostings().size() > 8) lines.add("외 " + (b.newPostings().size() - 8) + "건은 게시판에서");
            if (lines.isEmpty()) lines.add("새 공고 없음");
            out.add(section(markdown, "새 공고 " + b.newPostings().size() + "건 (접수 중 " + b.openPostingCount() + "건)", lines));
        }

        if (!b.closingSoon().isEmpty()) {
            out.add(section(markdown, "마감 임박 공고", b.closingSoon().stream()
                    .map(m -> Dday.label(b.today(), m.recruitment().applyEndDate()) + " " + m.recruitment().institutionName()
                            + " — " + ellipsis(m.recruitment().title(), 30))
                    .toList()));
        }

        if (!b.reminders().isEmpty()) {
            out.add(section(markdown, "오늘 챙길 D-day", b.reminders().stream().map(e -> eventLine(e, b.today())).toList()));
        }
        if (!b.upcoming().isEmpty()) {
            out.add(section(markdown, "다가오는 일정", b.upcoming().stream().map(e -> eventLine(e, b.today())).toList()));
        }

        if (!b.study().isEmpty()) {
            out.add(section(markdown, "이번 주 공부", b.study().stream().map(this::studyLine).toList()));
        }

        List<String> appLines = new ArrayList<>();
        if (!b.applications().countByStage().isEmpty()) {
            appLines.add(b.applications().countByStage().entrySet().stream()
                    .map(e -> e.getKey() + " " + e.getValue())
                    .collect(Collectors.joining(" · ")));
        }
        b.applications().dueSoon().forEach(a -> appLines.add(applicationLine(a, b.today())));
        if (!appLines.isEmpty()) out.add(section(markdown, "지원 현황", appLines));

        List<SpecStatus> alerts = b.specAlerts();
        if (!alerts.isEmpty()) {
            out.add(section(markdown, "스펙 알림", alerts.stream()
                    .map(s -> s.spec().name() + " " + s.state().label() + " (" + s.expiresOn() + ", "
                            + Dday.label(b.today(), s.expiresOn()) + ")")
                    .toList()));
        }
        return out;
    }

    private String eventLine(ScheduleEvent e, LocalDate today) {
        return Dday.label(today, e.date()) + " " + e.name() + " (" + e.date() + " " + e.kind().label() + ")"
                + (e.memo().isBlank() ? "" : " — " + e.memo());
    }

    private String studyLine(StudyProgress p) {
        return p.subject() + " " + p.label() + " (" + Math.round(p.ratio() * 100) + "%)";
    }

    private String applicationLine(Application a, LocalDate today) {
        StringBuilder sb = new StringBuilder(a.institution()).append(" [").append(a.stage()).append("]");
        if (a.deadline() != null) sb.append(" 마감 ").append(Dday.label(today, a.deadline()));
        if (a.nextDate() != null) sb.append(" · 다음 일정 ").append(Dday.label(today, a.nextDate()));
        return sb.toString();
    }

    private static String section(boolean markdown, String heading, List<String> lines) {
        String head = markdown ? "## " + heading : "■ " + heading;
        String bullet = markdown ? "- " : " · ";
        return head + "\n" + lines.stream().map(l -> bullet + l).collect(Collectors.joining("\n"));
    }

    private static String date(LocalDate d) {
        return d == null ? "?" : d.toString();
    }

    static String dateLabel(LocalDate d) {
        return d.getMonthValue() + "/" + d.getDayOfMonth() + "(" + d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN) + ")";
    }
}
