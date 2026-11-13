package com.dongju.jobits.job;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API 서버 측 필터 조건. 코드값은 '채용정보API_코드정의서'(MOEF_NKOD_DB_05) 기준.
 *
 * <ul>
 *   <li>ncsCdLst   R600020 = 정보통신 (전산직)</li>
 *   <li>hireTypeLst R1010 = 정규직, R1030 = 무기계약직, R1070 = 청년인턴(채용형)</li>
 *   <li>recrutSe   R2010 = 신입, R2020 = 경력, R2030 = 신입+경력</li>
 *   <li>workRgnLst R3010 = 서울, R3017 = 경기 ...</li>
 * </ul>
 */
public record RecruitmentSearchCondition(
        String ncsCode,
        String hireTypeCode,
        String recruitTypeCode,
        String workRegionCode,
        boolean onlyOngoing,
        int pageNo,
        int numOfRows
) {

    public static final String NCS_INFORMATION_COMMUNICATION = "R600020";
    public static final String HIRE_TYPE_REGULAR = "R1010";

    /** 기본값: 정보통신(전산) 분야 · 정규직 · 접수 중인 공고 · 100건 */
    public static RecruitmentSearchCondition itRegularOngoing() {
        return new RecruitmentSearchCondition(
                NCS_INFORMATION_COMMUNICATION, HIRE_TYPE_REGULAR, null, null, true, 1, 100);
    }

    /** profile.yml 의 job 설정으로 서버 필터를 만든다. 접수 중인 공고만. */
    public static RecruitmentSearchCondition from(com.dongju.jobits.config.Profile.JobPreference pref) {
        String ncs = pref.ncsCode() == null || pref.ncsCode().isBlank() ? NCS_INFORMATION_COMMUNICATION : pref.ncsCode();
        return new RecruitmentSearchCondition(ncs, pref.hireTypeCode(), null, null, true, 1, 100);
    }

    public RecruitmentSearchCondition withPage(int page) {
        return new RecruitmentSearchCondition(
                ncsCode, hireTypeCode, recruitTypeCode, workRegionCode, onlyOngoing, page, numOfRows);
    }

    public Map<String, String> toQueryParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("resultType", "json");
        params.put("pageNo", String.valueOf(pageNo));
        params.put("numOfRows", String.valueOf(numOfRows));
        putIfPresent(params, "ncsCdLst", ncsCode);
        putIfPresent(params, "hireTypeLst", hireTypeCode);
        putIfPresent(params, "recrutSe", recruitTypeCode);
        putIfPresent(params, "workRgnLst", workRegionCode);
        if (onlyOngoing) {
            params.put("ongoingYn", "Y");
        }
        return params;
    }

    private static void putIfPresent(Map<String, String> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.put(key, value);
        }
    }
}
