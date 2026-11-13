package com.dongju.jobits.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * config/ 폴더를 읽는다. 파일이 없으면 빈 설정으로 대체해서,
 * 일부 기능만 쓰는 사람도 바로 돌릴 수 있게 한다.
 */
public class LoadConfigService {

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory())
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);

    private final Path configDir;

    public LoadConfigService(Path configDir) {
        this.configDir = configDir;
    }

    public JobItsConfig load() throws IOException {
        Profile profile = readYaml("profile.yml", Profile.class);
        ScheduleConfig schedule = readYaml("schedule.yml", ScheduleConfig.class);
        ApplicationsConfig applications = readYaml("applications.yml", ApplicationsConfig.class);
        return new JobItsConfig(
                profile == null ? new Profile(null, null, null, null) : profile,
                schedule == null ? ScheduleConfig.empty() : schedule,
                applications == null ? ApplicationsConfig.empty() : applications,
                readStudyLog());
    }

    /** `log` 명령: study-log.csv 에 한 줄 추가 */
    public void appendStudyLog(StudyLogEntry entry) throws IOException {
        Path file = configDir.resolve("study-log.csv");
        if (!Files.exists(file)) {
            Files.writeString(file, "date,subject,minutes,memo\n", StandardCharsets.UTF_8);
        }
        String line = entry.date() + "," + csv(entry.subject()) + "," + entry.minutes() + "," + csv(entry.memo()) + "\n";
        Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    private <T> T readYaml(String fileName, Class<T> type) throws IOException {
        Path file = configDir.resolve(fileName);
        if (!Files.exists(file) || Files.size(file) == 0) return null;
        return yaml.readValue(file.toFile(), type);
    }

    List<StudyLogEntry> readStudyLog() throws IOException {
        Path file = configDir.resolve("study-log.csv");
        if (!Files.exists(file)) return List.of();
        List<StudyLogEntry> entries = new ArrayList<>();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {           // 0번 줄은 헤더
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] cols = line.split(",", 4);
            if (cols.length < 3) continue;
            try {
                entries.add(new StudyLogEntry(
                        LocalDate.parse(cols[0].trim()),
                        unquote(cols[1].trim()),
                        Integer.parseInt(cols[2].trim()),
                        cols.length > 3 ? unquote(cols[3].trim()) : ""));
            } catch (RuntimeException e) {
                System.err.println("[study-log] " + (i + 1) + "번째 줄을 건너뜀: " + line);
            }
        }
        return entries;
    }

    private static String csv(String s) {
        if (s == null) return "";
        return s.contains(",") || s.contains("\"") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1).replace("\"\"", "\"");
        }
        return s;
    }
}
