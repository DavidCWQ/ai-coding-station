package com.cwq.project_aicodingstation.ai.tool.fetcher;

import com.cwq.project_aicodingstation.ai.tool.dto.InterviewQuestion;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class MianshiyaFetcher implements InterviewSiteFetcher {

    private static final Pattern PREFIX = Pattern.compile("^\\s*(\\d+\\.)+\\s*");

    @Override
    public String siteName() {
        return "Mianshiya.com";
    }

    private String cleanTitle(String raw) {
        return PREFIX.matcher(raw).replaceFirst("").trim();
    }

    @Override
    public List<InterviewQuestion> fetch(String keyword) throws IOException {
        politeSleep(600);
        String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String url = "https://www.mianshiya.com/search/all?searchText=" + encoded;

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(5000)
                .referrer("https://www.google.com")
                .get();

        Elements links = doc.select("td.ant-table-cell > a[href^=/question/]");
        List<InterviewQuestion> result = new ArrayList<>();
        for (Element link : links) {
            result.add(new InterviewQuestion(
                    cleanTitle(link.text()),
                    "https://www.mianshiya.com" + link.attr("href"),
                    siteName()
            ));
        }
        log.debug("Mianshiya fetched count={}", result.size());
        return result;
    }
}
