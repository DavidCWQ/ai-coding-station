package com.cwq.project_aicodingstation.common.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {

    private Long id;

    @Serial
    private static final long serialVersionUID = 1L;
}
