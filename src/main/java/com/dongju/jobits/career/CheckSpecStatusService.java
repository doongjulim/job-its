package com.dongju.jobits.career;

import com.dongju.jobits.config.Profile.Spec;
import com.dongju.jobits.support.Dday;

import java.time.LocalDate;
import java.util.List;

/** 스펙 현황: 보유 / 준비 중 / 만료 임박(90일) / 만료 */
public class CheckSpecStatusService {

    static final int EXPIRY_WARNING_DAYS = 90;

    public List<SpecStatus> check(List<Spec> specs, LocalDate today) {
        return specs.stream().map(s -> statusOf(s, today)).toList();
    }

    SpecStatus statusOf(Spec spec, LocalDate today) {
        if (spec.acquired() == null) {
            return new SpecStatus(spec, State.PREPARING, null);
        }
        LocalDate expires = spec.expiresOn();
        if (expires == null) {
            return new SpecStatus(spec, State.VALID, null);
        }
        long left = Dday.between(today, expires);
        if (left < 0) return new SpecStatus(spec, State.EXPIRED, expires);
        if (left <= EXPIRY_WARNING_DAYS) return new SpecStatus(spec, State.EXPIRING, expires);
        return new SpecStatus(spec, State.VALID, expires);
    }

    public enum State {
        VALID("보유"), PREPARING("준비 중"), EXPIRING("만료 임박"), EXPIRED("만료");

        private final String label;

        State(String label) { this.label = label; }

        public String label() { return label; }
    }

    public record SpecStatus(Spec spec, State state, LocalDate expiresOn) {

        public boolean needsAttention() {
            return state == State.EXPIRING || state == State.EXPIRED;
        }
    }
}
