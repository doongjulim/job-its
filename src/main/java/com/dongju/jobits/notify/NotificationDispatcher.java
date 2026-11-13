package com.dongju.jobits.notify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 메시지를 켜진 모든 채널로 보낸다. 한 채널이 실패해도 나머지는 계속 보내고,
 * 실패 목록만 모아서 돌려준다(디스코드가 죽었다고 텔레그램까지 안 가면 안 되니까).
 */
public class NotificationDispatcher {

    private final List<Notifier> notifiers;

    public NotificationDispatcher(List<Notifier> notifiers) {
        this.notifiers = List.copyOf(notifiers);
    }

    public static NotificationDispatcher fromEnvironment(Map<String, String> env, JsonPoster poster) {
        List<Notifier> list = new ArrayList<>();
        list.add(new ConsoleNotifier());
        if (present(env, "DISCORD_WEBHOOK_URL")) {
            list.add(new DiscordWebhookNotifier(env.get("DISCORD_WEBHOOK_URL"), poster));
        }
        if (present(env, "TELEGRAM_BOT_TOKEN") && present(env, "TELEGRAM_CHAT_ID")) {
            list.add(new TelegramNotifier(env.get("TELEGRAM_BOT_TOKEN"), env.get("TELEGRAM_CHAT_ID"), poster));
        }
        if ("true".equalsIgnoreCase(env.get("JOBITS_ISSUES"))
                && present(env, "GITHUB_TOKEN") && present(env, "GITHUB_REPOSITORY")) {
            list.add(new GithubIssueNotifier(env.get("GITHUB_REPOSITORY"), env.get("GITHUB_TOKEN"), poster));
        }
        return new NotificationDispatcher(list);
    }

    public List<String> channelNames() {
        return notifiers.stream().map(Notifier::name).toList();
    }

    public Report dispatch(List<NotificationMessage> messages) {
        int sent = 0;
        List<String> failures = new ArrayList<>();
        for (NotificationMessage message : messages) {
            for (Notifier notifier : notifiers) {
                if (!notifier.supports(message.kind())) continue;
                try {
                    notifier.send(message);
                    sent++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failures.add(notifier.name() + ": interrupted");
                    return new Report(sent, failures);
                } catch (Exception e) {
                    failures.add(notifier.name() + ": " + e.getMessage());
                }
            }
        }
        return new Report(sent, failures);
    }

    private static boolean present(Map<String, String> env, String key) {
        String v = env.get(key);
        return v != null && !v.isBlank();
    }

    public record Report(int sent, List<String> failures) {}
}
