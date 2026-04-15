package com.cwq.project_aicodingstation.ai.tool.impl;

import com.cwq.project_aicodingstation.ai.tool.fetcher.InterviewSiteFetcher;
import com.cwq.project_aicodingstation.ai.tool.dto.InterviewQuestion;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InterviewQuestionTool {

    private final List<InterviewSiteFetcher> fetchers;

    public InterviewQuestionTool(List<InterviewSiteFetcher> fetchers) {
        this.fetchers = fetchers;
    }

    @Tool(
            name = "interviewQuestionSearch",
            value = """
                    Search interview questions from multiple Chinese interview platforms.
                    Use when the user asks for interview questions about a specific technology
                    or concept. Input should be a concise keyword.
                    """
    )
    public String searchInterviewQuestions(
            @P("keyword to search, e.g. redis, java multithreading")
            String keyword
    ) {
        List<InterviewQuestion> all = new ArrayList<>();
        for (InterviewSiteFetcher f : fetchers) {
            try {
                all.addAll(f.fetch(keyword));
            } catch (Exception e) {
                log.warn("Fetcher {} failed: {}", f.siteName(), e.getMessage());
            }
        }
        if (all.isEmpty()) {
            return "No interview questions found for: " + keyword;
        }
        return all.stream()
                .limit(10)
                .map(q -> String.format("• %s%n  Source: %s%n  Link: %s",
                        q.getTitle(), q.getSource(), q.getUrl()))
                .collect(Collectors.joining("\n\n"));
    }
}
