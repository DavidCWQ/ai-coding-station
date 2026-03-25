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
 * @apiNote ChatSession Entity Layer
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_session")
public class ChatSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;
    private Long appId;
    private Long userId;

    private String title;   // 会话标题

    private LocalDateTime lastMsgTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Column(isLogicDelete = true)
    private Integer isDeleted;
}