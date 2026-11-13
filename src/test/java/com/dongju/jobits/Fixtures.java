package com.dongju.jobits;

import com.dongju.jobits.config.ApplicationsConfig;
import com.dongju.jobits.config.JobItsConfig;
import com.dongju.jobits.config.Profile;
import com.dongju.jobits.config.ScheduleConfig;
import com.dongju.jobits.job.Recruitment;

import java.time.LocalDate;
import java.util.List;

/** 테스트용 객체 빌더 */
public final class Fixtures {

    private Fixtures() {}

    public static Recruitment recruitment(String id, String inst, String title, LocalDate end,
                                          String recruitType, List<String> regions, List<String> hireTypes) {
        return new Recruitment(id, inst, title, end == null ? null : end.minusDays(14), end,
                List.of("정보통신"), hireTypes, regions, recruitType, "3", "https://job.alio.go.kr/" + id, true);
    }

    public static Recruitment recruitment(String id, String inst, String title, LocalDate end) {
        return recruitment(id, inst, title, end, "신입", List.of("서울"), List.of("정규직"));
    }

    public static Profile.JobPreference preference() {
        return new Profile.JobPreference("R600020", "R1010",
                List.of("한국전력", "국민연금"), List.of("전산", "IT"), List.of("서울", "경기"),
                List.of("신입"), 30);
    }

    public static JobItsConfig config(ScheduleConfig schedule, ApplicationsConfig apps) {
        Profile profile = new Profile("DJ", "공기업 전산직 합격", preference(), List.of());
        return new JobItsConfig(profile, schedule, apps, List.of());
    }
}
