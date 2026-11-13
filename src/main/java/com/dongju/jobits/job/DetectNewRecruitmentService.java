package com.dongju.jobits.job;

import com.dongju.jobits.state.SeenRecruitmentStore;

import java.util.List;

/** 지난 실행 이후 새로 올라온 공고를 골라내고, 본 목록에 기록한다. */
public class DetectNewRecruitmentService {

    private final SeenRecruitmentStore store;

    public DetectNewRecruitmentService(SeenRecruitmentStore store) {
        this.store = store;
    }

    public Result detect(List<MatchedRecruitment> current) {
        List<MatchedRecruitment> fresh = current.stream()
                .filter(m -> !store.contains(keyOf(m.recruitment())))
                .toList();
        store.addAll(current.stream().map(m -> keyOf(m.recruitment())).toList());
        return new Result(fresh, store.isFirstRun());
    }

    /** 공고 일련번호가 있으면 그걸, 없으면 기관|제목|마감일 조합을 키로 쓴다. */
    public static String keyOf(Recruitment r) {
        if (r.id() != null && !r.id().isBlank()) return "id:" + r.id();
        return "k:" + r.institutionName() + "|" + r.title() + "|" + r.applyEndDate();
    }

    /**
     * @param newOnes  이번에 처음 본 공고
     * @param firstRun 상태 파일 없이 시작한 첫 실행이면 true — 개별 알림 대신 요약만 보낸다
     */
    public record Result(List<MatchedRecruitment> newOnes, boolean firstRun) {}
}
