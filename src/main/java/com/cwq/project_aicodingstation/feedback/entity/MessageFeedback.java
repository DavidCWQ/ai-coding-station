package com.cwq.project_aicodingstation.feedback.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 一条消息上的评价。同一用户对同一条消息只保留一行，取消时删掉这一行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("message_feedback")
public class MessageFeedback implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    private Long userId;

    /**
     * app / agent
     */
    private String source;

    private Long messageId;

    /**
     * 1 有帮助，-1 没帮助
     */
    private Integer vote;

    private String reason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
