package com.dongju.jobits.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LoadConfigServiceTest {

    @TempDir
    Path dir;

    @Test
    void 레포에_들어있는_config_폴더를_그대로_읽을_수_있다() throws Exception {
        JobItsConfig config = new LoadConfigService(Path.of("config")).load();

        assertEquals("R600020", config.profile().job().ncsCode());
        assertFalse(config.profile().job().watchInstitutions().isEmpty());
        assertEquals(30, config.profile().job().notifyMinScore());
        assertFalse(config.schedule().events().isEmpty());
        assertNotNull(config.applications());
    }

    @Test
    void 파일이_없으면_빈_설정으로_대체한다() throws Exception {
        JobItsConfig config = new LoadConfigService(dir).load();
        assertTrue(config.schedule().events().isEmpty());
        assertTrue(config.applications().applications().isEmpty());
        assertTrue(config.studyLog().isEmpty());
        assertEquals(30, config.profile().job().notifyMinScore());
    }

    @Test
    void 모르는_kind는_ETC로_빈_값은_기본값으로() throws Exception {
        Files.writeString(dir.resolve("schedule.yml"), """
                events:
                  - name: 무언가
                    date: 2027-01-02
                    kind: SOMETHING
                studyPlans:
                  - subject: NCS
                    weeklyGoalMinutes: 300
                """);
        JobItsConfig config = new LoadConfigService(dir).load();
        assertEquals(ScheduleConfig.EventKind.ETC, config.schedule().events().get(0).kind());
        assertEquals("", config.schedule().events().get(0).memo());
        assertTrue(config.schedule().studyPlans().get(0).isActiveOn(LocalDate.of(2030, 1, 1)));
    }

    @Test
    void 공부_기록을_추가하고_다시_읽는다_쉼표가_든_메모도() throws Exception {
        LoadConfigService service = new LoadConfigService(dir);
        service.appendStudyLog(new StudyLogEntry(LocalDate.of(2026, 11, 13), "TOEIC", 90, "RC, 파트5"));
        service.appendStudyLog(new StudyLogEntry(LocalDate.of(2026, 11, 13), "NCS", 40, ""));
        Files.writeString(dir.resolve("study-log.csv"), "잘못된줄\n", java.nio.file.StandardOpenOption.APPEND);

        var log = service.load().studyLog();

        assertEquals(2, log.size());
        assertEquals("RC, 파트5", log.get(0).memo());
        assertEquals(40, log.get(1).minutes());
    }
}
