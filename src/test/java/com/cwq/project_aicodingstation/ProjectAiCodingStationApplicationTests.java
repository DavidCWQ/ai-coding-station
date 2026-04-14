package com.cwq.project_aicodingstation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import com.cwq.project_aicodingstation.app.task.DeletedAppPurgeJob;

@SpringBootTest
@ActiveProfiles("test")
class ProjectAiCodingStationApplicationTests {

    @MockitoBean
    private ChatModel myChatModel;

    @MockitoBean
    private StreamingChatModel myStreamingChatModel;

    @MockitoBean
    private RedisChatMemoryStore redisChatMemoryStore;

    @MockitoBean
    private DeletedAppPurgeJob deletedAppPurgeJob;

    @Test
    void contextLoads() {
    }

}
