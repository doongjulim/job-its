package com.dongju.jobits.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** 잡알리오 API 를 페이지 단위로 끝까지(최대 maxPages) 읽는다. */
public class AlioRecruitmentSource implements RecruitmentSource {

    private final FetchAlioRecruitmentClient client;
    private final ParseRecruitmentService parser;
    private final int maxPages;

    public AlioRecruitmentSource(FetchAlioRecruitmentClient client, ParseRecruitmentService parser, int maxPages) {
        this.client = client;
        this.parser = parser;
        this.maxPages = maxPages;
    }

    @Override
    public List<Recruitment> fetchAll(RecruitmentSearchCondition condition) throws IOException, InterruptedException {
        List<Recruitment> all = new ArrayList<>();
        for (int page = 1; page <= maxPages; page++) {
            ParseRecruitmentService.ParsedPage parsed = parser.parse(client.fetchRawJson(condition.withPage(page)));
            all.addAll(parsed.items());
            if (parsed.items().isEmpty() || all.size() >= parsed.totalCount()) break;
        }
        return all;
    }
}
