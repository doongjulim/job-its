package com.dongju.jobits.job;

import java.io.IOException;
import java.util.List;

/** 공고를 어디서 가져오는지 숨긴다. 실서비스는 잡알리오 API, 데모·테스트는 픽스처 파일. */
public interface RecruitmentSource {

    List<Recruitment> fetchAll(RecruitmentSearchCondition condition) throws IOException, InterruptedException;
}
