package com.dongju.jobits.support;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class Dday {

    private Dday() {}

    public static long between(LocalDate today, LocalDate target) {
        return ChronoUnit.DAYS.between(today, target);
    }

    /** D-12 / D-DAY / D+3 */
    public static String label(LocalDate today, LocalDate target) {
        if (target == null) return "-";
        long d = between(today, target);
        if (d == 0) return "D-DAY";
        return d > 0 ? "D-" + d : "D+" + (-d);
    }
}
