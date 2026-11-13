package com.dongju.jobits.notify;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

/** 디스코드 채널 웹훅. 채널 설정 > 연동 > 웹후크에서 URL 발급 → DISCORD_WEBHOOK_URL */
public class DiscordWebhookNotifier implements Notifier {

    static final int MAX_LENGTH = 2000;

    private final String webhookUrl;
    private final JsonPoster poster;
    private final ObjectMapper mapper = new ObjectMapper();

    public DiscordWebhookNotifier(String webhookUrl, JsonPoster poster) {
        this.webhookUrl = webhookUrl;
        this.poster = poster;
    }

    @Override
    public String name() { return "discord"; }

    @Override
    public boolean supports(NotificationMessage.Kind kind) { return true; }

    @Override
    public void send(NotificationMessage message) throws IOException, InterruptedException {
        String content = truncate(message.asText(), MAX_LENGTH);
        String json = mapper.writeValueAsString(Map.of("content", content, "username", "잡있으"));
        int status = poster.post(webhookUrl, Map.of(), json);
        if (status / 100 != 2) throw new IOException("discord HTTP " + status);
    }

    static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
