package com.dongju.jobits.render;

import com.dongju.jobits.config.ScheduleConfig.ScheduleEvent;
import com.dongju.jobits.schedule.SummarizeStudyProgressService.StudyProgress;
import com.dongju.jobits.support.Dday;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static com.dongju.jobits.support.Texts.ellipsis;
import static com.dongju.jobits.support.Texts.xml;

/** generated/schedule.svg — 다가오는 일정 D-day + 이번 주 공부 진행률 */
public class RenderScheduleCardService {

    static final int WIDTH = 500;
    static final int EVENT_ROW = 24;
    static final int STUDY_ROW = 26;
    static final int BAR_X = 120;
    static final int BAR_WIDTH = 260;

    public String render(String goal, List<ScheduleEvent> events, List<StudyProgress> study, LocalDate today) {
        int eventRows = Math.max(1, events.size());
        int studyRows = Math.max(1, study.size());
        int eventsTop = 92;
        int studyHead = eventsTop + eventRows * EVENT_ROW + 22;
        int studyTop = studyHead + 14;
        int height = studyTop + studyRows * STUDY_ROW + 16;

        StringBuilder svg = new StringBuilder(SvgStyle.open(WIDTH, height, "잡있으 일정과 공부"));
        svg.append("<text x=\"20\" y=\"32\" class=\"title\">잡있으 · 일정 &amp; 공부</text>\n");
        svg.append("<text x=\"20\" y=\"52\" class=\"sub\">")
                .append(xml(ellipsis(goal == null || goal.isBlank() ? today.toString() : goal, 46)))
                .append("</text>\n");

        svg.append("<text x=\"20\" y=\"").append(eventsTop - 6).append("\" class=\"head\">다가오는 일정</text>\n");
        if (events.isEmpty()) {
            svg.append("<text x=\"20\" y=\"").append(eventsTop + 16).append("\" class=\"text\">등록된 일정이 없어요</text>\n");
        }
        for (int i = 0; i < events.size(); i++) {
            ScheduleEvent e = events.get(i);
            int y = eventsTop + 16 + i * EVENT_ROW;
            long d = Dday.between(today, e.date());
            svg.append(SvgStyle.rowOpen(i));
            svg.append("<text x=\"20\" y=\"").append(y).append("\" class=\"text ")
                    .append(d <= 7 ? "warn" : "accent").append("\">").append(Dday.label(today, e.date())).append("</text>");
            svg.append("<text x=\"84\" y=\"").append(y).append("\" class=\"text\">")
                    .append(xml(ellipsis(e.name(), 26))).append("</text>");
            svg.append("<text x=\"").append(WIDTH - 20).append("\" y=\"").append(y)
                    .append("\" class=\"small\" text-anchor=\"end\">")
                    .append(e.date().getMonthValue()).append("/").append(e.date().getDayOfMonth())
                    .append(" · ").append(e.kind().label()).append("</text>");
            svg.append("</g>\n");
        }

        svg.append("<text x=\"20\" y=\"").append(studyHead).append("\" class=\"head\">이번 주 공부</text>\n");
        if (study.isEmpty()) {
            svg.append("<text x=\"20\" y=\"").append(studyTop + 16).append("\" class=\"text\">이번 주 활성화된 공부 계획이 없어요</text>\n");
        }
        for (int i = 0; i < study.size(); i++) {
            StudyProgress p = study.get(i);
            int y = studyTop + i * STUDY_ROW;
            int fill = (int) Math.round(BAR_WIDTH * p.ratio());
            svg.append(SvgStyle.rowOpen(events.size() + i));
            svg.append("<text x=\"20\" y=\"").append(y + 13).append("\" class=\"text\">")
                    .append(xml(ellipsis(p.subject(), 8))).append("</text>");
            svg.append("<rect x=\"").append(BAR_X).append("\" y=\"").append(y + 4)
                    .append("\" width=\"").append(BAR_WIDTH).append("\" height=\"10\" rx=\"5\" fill=\"")
                    .append(SvgStyle.TRACK).append("\"/>");
            if (fill > 0) {
                svg.append("<rect x=\"").append(BAR_X).append("\" y=\"").append(y + 4)
                        .append("\" width=\"").append(Math.max(fill, 10)).append("\" height=\"10\" rx=\"5\" fill=\"")
                        .append(SvgStyle.ACCENT).append("\"/>");
            }
            svg.append("<text x=\"").append(WIDTH - 20).append("\" y=\"").append(y + 13)
                    .append("\" class=\"small\" text-anchor=\"end\">").append(p.label())
                    .append(String.format(Locale.ROOT, " (%d%%)", Math.round(p.ratio() * 100))).append("</text>");
            svg.append("</g>\n");
        }
        svg.append(SvgStyle.close());
        return svg.toString();
    }
}
