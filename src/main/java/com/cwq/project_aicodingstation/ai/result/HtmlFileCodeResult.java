package com.cwq.project_aicodingstation.ai.result;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
@Description("Generate HTML Code result")
public class HtmlFileCodeResult {

    @Description("Html Code")
    private String htmlCode;

    @Description("Code Description")
    private String description;

}
