package com.dongju.jobits.notify;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

/** 텔레그램 봇. @BotFather 로 토큰 발급 → TELEGRAM_BOT_TOKEN, 내 chat id → TELEGRAM_CHAT_ID */
public class TelegramNotifier implements Notifier {

    static final int MAX_LENGTH = 4096;

    private final String botToken;
    private final String chatId;
    private final JsonPoster poster;
    private final ObjectMapper mapper = new ObjectMapper();

    public TelegramNotifier(String botToken, String chatId, JsonPoster poster) {
        this.botToken = botToken;
        this.chatId = chatId;
        this.poster = poster;
    }

    @Override
    public String name() { return "telegram"; }

    @Override
    public boolean supports(NotificationMessage.Kind kind) { return true; }

    @Override
    public void send(NotificationMessage message) throws IOException, InterruptedException {
        String text = DiscordWebhookNotifier.truncate(message.asText(), MAX_LENGTH);
        String json = mapper.writeValueAsString(Map.of(
                "chat_id", chatId,
                "text", text,
                "disable_web_page_preview", true));
        int status = poster.post("https://api.telegram.org/bot" + botToken + "/sendMessage", Map.of(), json);
        if (status / 100 != 2) throw new IOException("telegram HTTP " + status);
    }
}
