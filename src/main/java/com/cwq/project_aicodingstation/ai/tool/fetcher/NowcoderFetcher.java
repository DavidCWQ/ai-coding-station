package com.cwq.project_aicodingstation.ai.tool.fetcher;

import com.cwq.project_aicodingstation.ai.tool.dto.InterviewQuestion;
import com.cwq.project_aicodingstation.ai.tool.config.InterviewSiteHttpConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class NowcoderFetcher implements InterviewSiteFetcher {

    private static final String API = "https://gw-c.nowcoder.com/api/sparta/pc/search";

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public NowcoderFetcher(@Qualifier(InterviewSiteHttpConfig.INTERVIEW_SITE_REST_TEMPLATE) RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String siteName() {
        return "Nowcoder.com";
    }

    @Override
    public List<InterviewQuestion> fetch(String keyword) {
        politeSleep(600);

        // ---------- Headers ---------------
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

        // ---------- Request Body ----------
        Map<String, Object> body = new HashMap<>();
        body.put("type", "all");
        body.put("query", keyword);
        body.put("tag", Collections.emptyList());
        body.put("page", 1);
        body.put("order", "relevant");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(API, HttpMethod.POST, request, String.class);
        } catch (Exception e) {
            log.warn("Nowcoder request failed: {}", e.getMessage());
            return Collections.emptyList();
        }

        List<InterviewQuestion> results = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(response.getBody());
            if (!root.path("success").asBoolean(false)) {
                return results;
            }
            JsonNode records = root.path("data").path("records");
            for (JsonNode record : records) {
                JsonNode dataNode = record.path("data");
                boolean isMomentData = dataNode.has("momentData");
                JsonNode contentNode = isMomentData ? dataNode.path("momentData") : dataNode.path("contentData");
                if (contentNode.isMissingNode()) {
                    continue;
                }
                String title = contentNode.path("title").asText(null);
                if (title == null || title.isBlank()) {
                    continue;
                }
                long id = contentNode.path("id").asLong(-1);
                if (id <= 0) {
                    continue;
                }
                String uuid = contentNode.path("uuid").toString();
                String url = isMomentData
                        ? "https://www.nowcoder.com/feed/main/detail/" + uuid
                        : "https://www.nowcoder.com/discuss/" + id;
                results.add(new InterviewQuestion(title, url, siteName()));
            }
        } catch (Exception e) {
            log.warn("Nowcoder JSON parse failed: {}", e.getMessage());
        }
        log.debug("Nowcoder fetched count={}", results.size());
        return results;
    }
}
