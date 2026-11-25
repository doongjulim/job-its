package com.dongju.jobits.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 이미 본 공고 키 목록. GitHub Actions 에서는 output 브랜치의 state/seen-jobs.json 을
 * 실행 전에 복원하고 실행 후 다시 올린다(워크플로 참고).
 * 최근 MAX_KEYS 개만 유지해서 파일이 끝없이 커지지 않게 한다.
 */
public class SeenRecruitmentStore {

    public static final int MAX_KEYS = 3000;

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();
    private final LinkedHashSet<String> keys = new LinkedHashSet<>();
    private boolean firstRun;

    public SeenRecruitmentStore(Path file) {
        this.file = file;
    }

    public SeenRecruitmentStore load() throws IOException {
        keys.clear();
        if (Files.exists(file)) {
            JsonNode root = mapper.readTree(file.toFile());
            for (JsonNode key : root.path("keys")) {
                keys.add(key.asText());
            }
        }
        // 파일이 없거나, 있어도 비어 있으면(예: API 키 없이 돌던 기간) 첫 실행으로 본다
        firstRun = keys.isEmpty();
        return this;
    }

    /** 본 공고가 하나도 없던 첫 실행인가 — 첫 실행엔 기존 공고를 전부 '새 공고'로 알리지 않는다. */
    public boolean isFirstRun() {
        return firstRun;
    }

    public boolean contains(String key) {
        return keys.contains(key);
    }

    public void addAll(Collection<String> newKeys) {
        keys.addAll(newKeys);
        while (keys.size() > MAX_KEYS) {
            keys.remove(keys.iterator().next());
        }
    }

    public Set<String> keys() {
        return Set.copyOf(keys);
    }

    public void save() throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        ObjectNode root = mapper.createObjectNode();
        root.put("updatedAt", Instant.now().toString());
        ArrayNode arr = root.putArray("keys");
        List<String> ordered = new ArrayList<>(keys);
        ordered.forEach(arr::add);
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), root);
    }
}
