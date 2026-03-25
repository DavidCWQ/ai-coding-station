package com.cwq.project_aicodingstation.chat.entity;

import com.mybatisflex.annotation.Column;
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
 * @apiNote ChatHistory Entity Layer
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_history")
public class ChatHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    private String message;
    private String messageType; // 消息类型 user/ai/system
    private String fileList;    // 文件列表(JSON字符串)

    private Long appId;
    private Long userId;
    private Long sessionId;     // 会话id
    private Long parentId;      // 父消息id

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Column(isLogicDelete = true)
    private Integer isDeleted;
}
