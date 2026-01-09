package com.cwq.project_aicodingstation.common.request;

import lombok.Data;

@Data
public class PageRequest {

    // 当前页号（从 1 开始）
    private int pageNum = 1;

    // 页面大小
    private int pageSize = 10;

    // 排序字段
    private String sortField;

    // 排序顺序：ascend / descend
    private String sortOrder = "descend";
}
