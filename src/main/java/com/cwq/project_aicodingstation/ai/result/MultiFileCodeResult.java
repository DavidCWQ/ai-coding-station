package com.cwq.project_aicodingstation.ai.result;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
@Description("Generate Multi File Code result")
public class MultiFileCodeResult {

    @Description("Html Code")
    private String htmlCode;

    @Description("CSS Code")
    private String cssCode;

    @Description("Javascript Code")
    private String jsCode;

    @Description("Code Description")
    private String description;

}
