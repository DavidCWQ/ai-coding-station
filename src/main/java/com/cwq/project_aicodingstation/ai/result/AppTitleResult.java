package com.cwq.project_aicodingstation.ai.result;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
@Description("给网站应用起的短标题")
public class AppTitleResult {

    @Description("中文标题，2到12个字，不含标点、引号和解释")
    private String title;
}
