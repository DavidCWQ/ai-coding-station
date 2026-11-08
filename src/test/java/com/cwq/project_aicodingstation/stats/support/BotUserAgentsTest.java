package com.cwq.project_aicodingstation.stats.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotUserAgentsTest {

    @Test
    void blankUserAgentIsABot() {
        assertTrue(BotUserAgents.isBot(null));
        assertTrue(BotUserAgents.isBot("  "));
    }

    @Test
    void crawlerTokensAreBots() {
        assertTrue(BotUserAgents.isBot("curl/8.4.0"));
        assertTrue(BotUserAgents.isBot("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"));
        assertTrue(BotUserAgents.isBot("Mozilla/5.0 HeadlessChrome/120.0.0.0"));
    }

    @Test
    void ordinaryBrowserIsNotABot() {
        assertFalse(BotUserAgents.isBot(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));
    }
}
