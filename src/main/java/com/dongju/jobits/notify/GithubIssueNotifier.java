package com.dongju.jobits.notify;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 새 공고 1건 = 레포 이슈 1개(게시글). GitHub 모바일 앱이 있으면 푸시 알림까지 온다.
 * Actions 기본 GITHUB_TOKEN 으로 동작하므로 따로 토큰을 만들 필요가 없다.
 * 레포 Variables 에 JOBITS_ISSUES=true 를 넣어야 켜진다.
 */
public class GithubIssueNotifier implements Notifier {

    private final String repository;   // owner/repo
    private final String token;
    private final JsonPoster poster;
    private final ObjectMapper mapper = new ObjectMapper();

    public GithubIssueNotifier(String repository, String token, JsonPoster poster) {
        this.repository = repository;
        this.token = token;
        this.poster = poster;
    }

    @Override
    public String name() { return "github-issue"; }

    @Override
    public boolean supports(NotificationMessage.Kind kind) {
        return kind == NotificationMessage.Kind.POSTING;
    }

    @Override
    public void send(NotificationMessage message) throws IOException, InterruptedException {
        String body = message.body() + (message.url().isBlank() ? "" : "\n\n[공고 원문](" + message.url() + ")");
        String json = mapper.writeValueAsString(Map.of(
                "title", message.title(),
                "body", body,
                "labels", List.of("job-its", "공고")));
        int status = poster.post(
                "https://api.github.com/repos/" + repository + "/issues",
                Map.of("Authorization", "Bearer " + token,
                        "Accept", "application/vnd.github+json",
                        "X-GitHub-Api-Version", "2022-11-28"),
                json);
        if (status / 100 != 2) throw new IOException("github issue HTTP " + status);
    }
}
