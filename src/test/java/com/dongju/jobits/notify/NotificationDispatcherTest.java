package com.dongju.jobits.notify;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationDispatcherTest {

    private static final NotificationMessage POSTING =
            new NotificationMessage(NotificationMessage.Kind.POSTING, "새 공고", "본문", "https://x");
    private static final NotificationMessage BRIEFING =
            new NotificationMessage(NotificationMessage.Kind.BRIEFING, "브리핑", "본문", null);

    @Test
    void 한_채널이_실패해도_나머지_채널은_보낸다() {
        List<String> posted = new ArrayList<>();
        JsonPoster flaky = (url, headers, json) -> {
            if (url.contains("discord")) return 500;
            posted.add(url);
            return 200;
        };
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(
                new DiscordWebhookNotifier("https://discord.example/webhook", flaky),
                new TelegramNotifier("TOKEN", "42", flaky)));

        NotificationDispatcher.Report report = dispatcher.dispatch(List.of(BRIEFING));

        assertEquals(1, report.sent());
        assertEquals(1, report.failures().size());
        assertTrue(report.failures().get(0).startsWith("discord"));
        assertEquals(1, posted.size());
    }

    @Test
    void 깃허브_이슈는_새_공고만_받는다() {
        List<String> bodies = new ArrayList<>();
        JsonPoster capture = (url, headers, json) -> {
            bodies.add(json);
            assertEquals("Bearer t", headers.get("Authorization"));
            return 201;
        };
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(new GithubIssueNotifier("me/job-its", "t", capture)));

        NotificationDispatcher.Report report = dispatcher.dispatch(List.of(POSTING, BRIEFING));

        assertEquals(1, report.sent());
        assertTrue(bodies.get(0).contains("공고 원문"));
        assertTrue(bodies.get(0).contains("\"labels\""));
    }

    @Test
    void 환경변수에_있는_채널만_켠다() {
        JsonPoster noop = (u, h, j) -> 200;
        assertEquals(List.of("console"), NotificationDispatcher.fromEnvironment(Map.of(), noop).channelNames());

        NotificationDispatcher all = NotificationDispatcher.fromEnvironment(Map.of(
                "DISCORD_WEBHOOK_URL", "https://d",
                "TELEGRAM_BOT_TOKEN", "t", "TELEGRAM_CHAT_ID", "1",
                "JOBITS_ISSUES", "true", "GITHUB_TOKEN", "g", "GITHUB_REPOSITORY", "me/r"), noop);
        assertEquals(List.of("console", "discord", "telegram", "github-issue"), all.channelNames());

        // 텔레그램은 토큰과 chat id 둘 다 있어야 켜진다
        assertEquals(List.of("console"),
                NotificationDispatcher.fromEnvironment(Map.of("TELEGRAM_BOT_TOKEN", "t"), noop).channelNames());
    }

    @Test
    void 디스코드는_2000자로_자른다() throws IOException, InterruptedException {
        List<String> sent = new ArrayList<>();
        new DiscordWebhookNotifier("https://d", (u, h, json) -> { sent.add(json); return 204; })
                .send(new NotificationMessage(NotificationMessage.Kind.BRIEFING, "t", "가".repeat(3000), ""));
        String content = new com.fasterxml.jackson.databind.ObjectMapper().readTree(sent.get(0)).get("content").asText();
        assertEquals(DiscordWebhookNotifier.MAX_LENGTH, content.length());
        assertTrue(content.endsWith("…"));
    }
}
