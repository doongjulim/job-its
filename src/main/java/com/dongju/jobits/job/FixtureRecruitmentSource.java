package com.dongju.jobits.job;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** 저장해 둔 API 응답 JSON 으로 동작한다. `demo` 명령과 테스트용. */
public class FixtureRecruitmentSource implements RecruitmentSource {

    private final Path jsonFile;
    private final ParseRecruitmentService parser;

    public FixtureRecruitmentSource(Path jsonFile, ParseRecruitmentService parser) {
        this.jsonFile = jsonFile;
        this.parser = parser;
    }

    @Override
    public List<Recruitment> fetchAll(RecruitmentSearchCondition condition) throws IOException {
        return parser.parse(Files.readString(jsonFile, StandardCharsets.UTF_8)).items();
    }
}
