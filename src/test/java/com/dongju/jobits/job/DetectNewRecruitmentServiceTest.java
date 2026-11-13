package com.dongju.jobits.job;

import com.dongju.jobits.Fixtures;
import com.dongju.jobits.state.SeenRecruitmentStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DetectNewRecruitmentServiceTest {

    private static final LocalDate END = LocalDate.of(2026, 12, 1);

    @TempDir
    Path dir;

    private MatchedRecruitment m(String id) {
        return new MatchedRecruitment(Fixtures.recruitment(id, "기관" + id, "공고" + id, END), 50, List.of());
    }

    @Test
    void 첫_실행은_firstRun으로_표시하고_다음_실행부터_새것만_돌려준다() throws Exception {
        Path file = dir.resolve("seen.json");

        SeenRecruitmentStore store1 = new SeenRecruitmentStore(file).load();
        DetectNewRecruitmentService.Result first = new DetectNewRecruitmentService(store1).detect(List.of(m("1"), m("2")));
        store1.save();
        assertTrue(first.firstRun());
        assertEquals(2, first.newOnes().size());

        SeenRecruitmentStore store2 = new SeenRecruitmentStore(file).load();
        DetectNewRecruitmentService.Result second = new DetectNewRecruitmentService(store2).detect(List.of(m("1"), m("2"), m("3")));
        assertFalse(second.firstRun());
        assertEquals(List.of("3"), second.newOnes().stream().map(x -> x.recruitment().id()).toList());
    }

    @Test
    void 일련번호가_없으면_기관_제목_마감일로_키를_만든다() {
        Recruitment noId = Fixtures.recruitment("", "한국전력공사", "전산", END);
        assertEquals("k:한국전력공사|전산|2026-12-01", DetectNewRecruitmentService.keyOf(noId));
        assertEquals("id:7", DetectNewRecruitmentService.keyOf(Fixtures.recruitment("7", "a", "b", END)));
    }

    @Test
    void 저장_키는_최근_MAX_KEYS개만_유지한다() {
        SeenRecruitmentStore store = new SeenRecruitmentStore(dir.resolve("x.json"));
        for (int i = 0; i < SeenRecruitmentStore.MAX_KEYS + 5; i++) {
            store.addAll(List.of("k" + i));
        }
        assertEquals(SeenRecruitmentStore.MAX_KEYS, store.keys().size());
        assertFalse(store.contains("k0"));
        assertTrue(store.contains("k" + (SeenRecruitmentStore.MAX_KEYS + 4)));
    }
}
