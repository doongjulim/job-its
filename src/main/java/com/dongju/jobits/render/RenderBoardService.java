package com.dongju.jobits.render;

import com.dongju.jobits.job.DetectNewRecruitmentService;
import com.dongju.jobits.job.MatchedRecruitment;
import com.dongju.jobits.job.Recruitment;
import com.dongju.jobits.support.Dday;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dongju.jobits.support.Texts.markdownCell;
import static com.dongju.jobits.support.Texts.orDash;

/** generated/jobs.md — 접수 중 공고 전체 게시판 + generated/README.md 대시보드 */
public class RenderBoardService {

    public String board(List<MatchedRecruitment> postings, List<MatchedRecruitment> newOnes, LocalDate today) {
        Set<String> newKeys = newOnes.stream()
                .map(m -> DetectNewRecruitmentService.keyOf(m.recruitment()))
                .collect(Collectors.toSet());
        StringBuilder md = new StringBuilder();
        md.append("# 잡있으 공고 게시판\n\n");
        md.append("> ").append(today).append(" 기준 · 접수 중 ").append(postings.size())
                .append("건 · 맞춤 점수 높은 순, 같으면 마감 임박 순\n\n");
        if (postings.isEmpty()) {
            return md.append("조건에 맞는 접수 중 공고가 없어요.\n").toString();
        }
        md.append("| 맞춤 | 마감 | 기관 | 공고 | 구분 | 근무지 | 접수기간 |\n");
        md.append("|---|---|---|---|---|---|---|\n");
        for (MatchedRecruitment m : postings) {
            Recruitment r = m.recruitment();
            boolean isNew = newKeys.contains(DetectNewRecruitmentService.keyOf(r));
            String title = markdownCell(r.title());
            String linked = r.url().isBlank() ? title : "[" + title + "](" + r.url() + ")";
            md.append("| ").append(m.stars()).append(' ').append(m.score())
                    .append(" | ").append(Dday.label(today, r.applyEndDate()))
                    .append(" | ").append(markdownCell(r.institutionName()))
                    .append(" | ").append(isNew ? "🆕 " : "").append(linked)
                    .append(" | ").append(markdownCell(orDash(r.recruitType())))
                    .append(" | ").append(markdownCell(r.workRegions().isEmpty() ? "-" : String.join(", ", r.workRegions())))
                    .append(" | ").append(r.applyStartDate() == null ? "?" : r.applyStartDate())
                    .append(" ~ ").append(r.applyEndDate() == null ? "?" : r.applyEndDate())
                    .append(" |\n");
        }
        return md.toString();
    }

    /** output 브랜치 첫 화면 */
    public String dashboard(LocalDate today) {
        return """
                # 잡있으 대시보드

                %s 업데이트

                ![맞춤 공고](jobs.svg)
                ![일정과 공부](schedule.svg)

                - [공고 게시판](jobs.md)
                - [오늘의 브리핑](briefing.md)
                """.formatted(today);
    }
}
