package com.dongju.jobits.render;

import com.dongju.jobits.job.DetectNewRecruitmentService;
import com.dongju.jobits.job.MatchedRecruitment;
import com.dongju.jobits.job.Recruitment;
import com.dongju.jobits.support.Dday;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dongju.jobits.support.Texts.ellipsis;
import static com.dongju.jobits.support.Texts.xml;

/** generated/jobs.svg — 맞춤 점수 상위 공고 카드 */
public class RenderJobsCardService {

    static final int WIDTH = 500;
    static final int MAX_ROWS = 6;
    static final int ROW_HEIGHT = 46;
    static final int HEADER = 74;

    public String render(List<MatchedRecruitment> postings, List<MatchedRecruitment> newOnes, LocalDate today) {
        Set<String> newKeys = newOnes.stream()
                .map(m -> DetectNewRecruitmentService.keyOf(m.recruitment()))
                .collect(Collectors.toSet());
        List<MatchedRecruitment> rows = postings.stream().limit(MAX_ROWS).toList();
        int height = HEADER + Math.max(1, rows.size()) * ROW_HEIGHT + 12;

        StringBuilder svg = new StringBuilder(SvgStyle.open(WIDTH, height, "잡있으 맞춤 공고"));
        svg.append("<text x=\"20\" y=\"32\" class=\"title\">잡있으 · 맞춤 공고</text>\n");
        svg.append("<text x=\"20\" y=\"52\" class=\"sub\">")
                .append(today).append(" 기준 · 접수 중 ").append(postings.size()).append("건 · 새 공고 ")
                .append(newOnes.size()).append("건</text>\n");

        if (rows.isEmpty()) {
            svg.append("<text x=\"20\" y=\"").append(HEADER + 24)
                    .append("\" class=\"text\">조건에 맞는 접수 중 공고가 없어요</text>\n");
        }
        for (int i = 0; i < rows.size(); i++) {
            MatchedRecruitment m = rows.get(i);
            Recruitment r = m.recruitment();
            int y = HEADER + i * ROW_HEIGHT;
            Long days = r.daysUntilDeadline(today);
            boolean urgent = days != null && days <= 3;
            boolean isNew = newKeys.contains(DetectNewRecruitmentService.keyOf(r));

            svg.append(SvgStyle.rowOpen(i));
            svg.append("<text x=\"20\" y=\"").append(y + 16).append("\" class=\"small accent\">")
                    .append(m.stars()).append("</text>");
            svg.append("<text x=\"62\" y=\"").append(y + 16).append("\" class=\"name\">")
                    .append(xml(ellipsis(r.institutionName(), 18))).append("</text>");
            if (isNew) {
                svg.append("<text x=\"").append(WIDTH - 86).append("\" y=\"").append(y + 16)
                        .append("\" class=\"small warn\" text-anchor=\"end\">NEW</text>");
            }
            svg.append("<text x=\"").append(WIDTH - 20).append("\" y=\"").append(y + 16)
                    .append("\" class=\"text ").append(urgent ? "warn" : "accent").append("\" text-anchor=\"end\">")
                    .append(r.applyEndDate() == null ? "-" : Dday.label(today, r.applyEndDate())).append("</text>");
            svg.append("<text x=\"62\" y=\"").append(y + 34).append("\" class=\"text\">")
                    .append(xml(ellipsis(r.title(), 32))).append("</text>");
            svg.append("</g>\n");
        }
        svg.append(SvgStyle.close());
        return svg.toString();
    }
}
