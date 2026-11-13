package com.dongju.jobits.support;

public final class Texts {

    private Texts() {}

    /** 최대 max 글자. 넘치면 … 로 자른다(코드포인트 기준이라 한글도 안전). */
    public static String ellipsis(String s, int max) {
        if (s == null) return "";
        int count = s.codePointCount(0, s.length());
        if (count <= max) return s;
        int end = s.offsetByCodePoints(0, Math.max(0, max - 1));
        return s.substring(0, end) + "…";
    }

    public static String xml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String markdownCell(String s) {
        return s == null ? "" : s.replace("|", "\\|").replace("\n", " ");
    }

    public static String orDash(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}
