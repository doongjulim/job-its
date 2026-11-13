package com.dongju.jobits.render;

/**
 * github-stats-transparent 와 같은 원칙: 배경 투명 + 라이트/다크 어디서나 읽히는 중간 톤.
 * README 에 <img> 로 박아도 테마를 안 탄다.
 */
final class SvgStyle {

    private SvgStyle() {}

    static final String ACCENT = "rgb(72, 148, 224)";
    static final String WARN = "rgb(229, 83, 75)";
    static final String TEXT = "rgb(145, 145, 145)";
    static final String LABEL = "rgb(139, 139, 139)";
    static final String TRACK = "rgba(139, 139, 139, 0.2)";

    static String open(int width, int height, String ariaLabel) {
        return """
                <svg width="%d" height="%d" viewBox="0 0 %d %d" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="%s">
                <style>
                  svg { font-family: -apple-system, BlinkMacSystemFont, "Apple SD Gothic Neo", "Malgun Gothic", "Noto Sans KR", "Segoe UI", Helvetica, Arial, sans-serif; }
                  .title { font-size: 15px; font-weight: 600; fill: %s; }
                  .sub { font-size: 11px; fill: %s; }
                  .head { font-size: 12px; font-weight: 600; fill: %s; }
                  .name { font-size: 13px; font-weight: 600; fill: %s; }
                  .text { font-size: 12px; fill: %s; }
                  .small { font-size: 11px; fill: %s; }
                  .accent { fill: %s; font-weight: 600; }
                  .warn { fill: %s; font-weight: 600; }
                  .row { animation: fadeIn 0.5s ease-in-out backwards; }
                  @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
                </style>
                <rect x="0.5" y="0.5" rx="6" width="%d" height="%d" fill="none" stroke="#8B8B8B22"/>
                """.formatted(width, height, width, height, ariaLabel,
                ACCENT, TEXT, LABEL, LABEL, TEXT, TEXT, ACCENT, WARN, width - 1, height - 1);
    }

    static String close() {
        return "</svg>\n";
    }

    static String rowOpen(int index) {
        return "<g class=\"row\" style=\"animation-delay: " + (index * 120) + "ms\">";
    }
}
