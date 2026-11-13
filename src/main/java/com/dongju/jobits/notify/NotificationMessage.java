package com.dongju.jobits.notify;

/**
 * @param kind  POSTING = 새 공고 1건(게시글로도 올라감), BRIEFING = 하루 요약
 * @param url   원문 링크(없으면 빈 문자열)
 */
public record NotificationMessage(Kind kind, String title, String body, String url) {

    public NotificationMessage {
        url = url == null ? "" : url;
    }

    public enum Kind { POSTING, BRIEFING }

    /** 메신저용 한 덩어리 텍스트 */
    public String asText() {
        StringBuilder sb = new StringBuilder(title).append('\n').append(body);
        if (!url.isBlank()) sb.append('\n').append(url);
        return sb.toString();
    }
}
