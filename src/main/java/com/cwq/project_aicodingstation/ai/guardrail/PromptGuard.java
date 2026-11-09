package com.cwq.project_aicodingstation.ai.guardrail;

import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 本地输入检查。注入说法和内容词都在进模型之前匹配，不访问网络。
 */
@Slf4j
@Component
public class PromptGuard {

    public static final String USER_MESSAGE = "这条内容无法发送";

    private static final int FLAG = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    private static final List<String> CONTENT_WORDS = List.of(
            "terrorism",
            "bombing",
            "genocide",
            "assassination",
            "massacre",
            "childporn",
            "rape",
            "suicide",
            "self-harm"
    );

    private static final List<Rule> RULES = buildRules();

    private static final List<Rule> CONTENT_RULES = CONTENT_WORDS.stream()
            .map(word -> rule("content." + word, "\\b" + Pattern.quote(word) + "\\b"))
            .toList();

    private final InputGuardrailProperties properties;

    public PromptGuard(InputGuardrailProperties properties) {
        this.properties = properties;
    }

    /**
     * 命中时抛出业务异常。被拒绝的原文不写入日志。
     */
    public void check(String text, Long userId, String scope) {
        String rule = matchRule(text);
        if (rule == null) {
            return;
        }
        int length = text == null ? 0 : text.length();
        log.warn("输入被护轨拒绝 userId={} scope={} rule={} length={}", userId, scope, rule, length);
        throw new BusinessException(ErrorCode.PROMPT_BLOCKED, USER_MESSAGE);
    }

    /**
     * @return 命中的规则编号；未命中或护轨关闭时返回 null
     */
    public String matchRule(String text) {
        if (!properties.isEnabled() || !StringUtils.hasText(text)) {
            return null;
        }
        for (Rule rule : RULES) {
            if (rule.pattern.matcher(text).find()) {
                return rule.id;
            }
        }
        String content = matchContentWord(text);
        if (content != null) {
            return content;
        }
        return matchExtra(text);
    }

    private String matchContentWord(String text) {
        for (Rule rule : CONTENT_RULES) {
            if (rule.pattern.matcher(text).find()) {
                return rule.id;
            }
        }
        return null;
    }

    private String matchExtra(String text) {
        for (String raw : properties.getSensitiveKeywords()) {
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            String word = raw.trim();
            Pattern pattern = ascii(word)
                    ? Pattern.compile("\\b" + Pattern.quote(word) + "\\b", FLAG)
                    : Pattern.compile(Pattern.quote(word), FLAG);
            if (pattern.matcher(text).find()) {
                return "content.extra";
            }
        }
        return null;
    }

    private static boolean ascii(String word) {
        return word.chars().allMatch(ch -> ch < 128);
    }

    private static List<Rule> buildRules() {
        List<Rule> rules = new ArrayList<>();
        rules.add(rule("inj.ignore",
                "ignore\\s+(?:(?:all|any|the|previous|prior|above|foregoing|your)\\s+)*(?:instructions?|rules?|prompts?|context|guidelines|directives|commands)"));
        rules.add(rule("inj.forget",
                "forget\\s+(?:everything|all\\s+(?:rules|instructions)|your\\s+(?:instructions|rules|prompt)|previous\\s+instructions|prior\\s+context)"));
        rules.add(rule("inj.disregard",
                "(?:disregard|dismiss)\\s+(?:(?:all|any|your|the|previous|prior|above)\\s+)*(?:instructions?|rules?|prompts?|context|guidelines)"));
        rules.add(rule("inj.override",
                "(?:override|overwrite)\\s+(?:(?:your|the|previous|system)\\s+)*(?:instructions?|rules?|system\\s+prompt|prompts?)"));
        rules.add(rule("inj.dont-follow",
                "(?:do\\s+not|don't|never)\\s+follow\\s+(?:(?:your|the|previous|prior|system)\\s+)*(?:instructions?|rules?|prompts?|guidelines)"));
        rules.add(rule("inj.stop-following",
                "(?:stop|cease)\\s+following\\s+(?:(?:your|the|previous|prior)\\s+)*(?:instructions?|rules?|prompts?)"));
        rules.add(rule("inj.discard",
                "discard\\s+(?:(?:your|the|previous|all)\\s+)*(?:instructions?|rules?|prompts?)"));
        rules.add(rule("inj.from-now",
                "from\\s+now\\s+on[\\s\\S]{0,40}?(?:ignore|disregard|forget|must\\s+not\\s+follow)"));
        rules.add(rule("inj.reveal",
                "(?:reveal|show|print|repeat|output|dump|leak|display|echo|quote|recite|translate)\\s+(?:me\\s+)?(?:your|the|hidden|original|initial|full|system|above)\\s+(?:system\\s+)?(?:prompts?|instructions?|rules?|guidelines|messages?)"));
        rules.add(rule("inj.what-are",
                "what\\s+(?:are|were|is)\\s+your\\s+(?:(?:original|initial|system|hidden)\\s+)?(?:instructions?|prompts?|rules?)"));
        rules.add(rule("inj.repeat-above",
                "repeat\\s+(?:everything|the\\s+text|the\\s+words)\\s+above"));
        rules.add(rule("inj.print-above",
                "print\\s+everything\\s+above"));
        rules.add(rule("inj.you-are-now",
                "you\\s+are\\s+now\\s+(?:dan|jailbroken|unfiltered|unrestricted)\\b"));
        rules.add(rule("inj.act-as",
                "act\\s+as\\s+(?:dan|an?\\s+unrestricted|an?\\s+unfiltered)\\b"));
        rules.add(rule("inj.pretend",
                "pretend\\s+(?:to\\s+be|you\\s+are)\\s+(?:an?\\s+ai\\s+)?(?:with\\s+no|without)\\s+(?:restrictions|rules|filters)"));
        rules.add(rule("inj.no-restrictions",
                "with\\s+no\\s+restrictions|without\\s+any\\s+restrictions|without\\s+safety\\s+filters|without\\s+ethical\\s+guidelines|no\\s+safety\\s+filters|no\\s+ethical\\s+guidelines"));
        rules.add(rule("inj.dan",
                "do\\s+anything\\s+now|\\bDAN\\s+mode\\b|\\bD\\.A\\.N\\."));
        rules.add(rule("inj.jailbreak",
                "\\bjailbreak\\b"));
        rules.add(rule("inj.bypass",
                "bypass\\s+(?:all\\s+)?(?:safety|content|security)\\s+(?:filters?|restrictions?|checks?)"));
        rules.add(rule("inj.ignore-safety",
                "ignore\\s+(?:all\\s+)?(?:safety|ethical)\\s+(?:filters?|guidelines|restrictions?)"));
        rules.add(rule("inj.fence",
                "```\\s*system"));
        rules.add(rule("inj.im-start",
                "<\\|im_start\\|>|<\\|im_end\\|>"));
        rules.add(rule("inj.inst",
                "\\[/?INST]"));
        rules.add(rule("inj.sys-tag",
                "<<\\/?SYS>>"));
        rules.add(rule("inj.decode",
                "decode\\s+(?:the\\s+following|this)\\s+and\\s+(?:execute|run|follow|obey)"));
        rules.add(rule("inj.base64",
                "base64[\\s\\S]{0,24}?(?:decode|解码)[\\s\\S]{0,8}?(?:execute|run|执行)"));
        rules.add(rule("inj.admin",
                "the\\s+(?:admin|developer|system)\\s+(?:has\\s+)?(?:authorized|allowed)\\s+you\\s+to\\s+ignore"));
        rules.add(rule("zh.admin",
                "(?:管理员|开发者|系统)[\\s\\S]{0,12}?(?:允许你忽略|授权你忽略)"));
        rules.add(rule("zh.discard",
                "(?:不要再遵守|停止遵守|不必遵守|不用遵守|不要遵守|勿遵守|不要管|别管|忽略|无视)"
                        + "(?:之前的?|以上的?|上面的?|前面的?|先前的?|原先的?|原来的?|所有的?|全部的?|你的)?"
                        + "(?:系统提示词|系统提示|提示词|指令|设定|规则)"));
        rules.add(rule("zh.discard-system",
                "(?:不要再遵守|停止遵守|不必遵守|不用遵守|不要遵守|勿遵守|不要管|别管|忽略|无视)系统(?:指令|设定|规则)"));
        rules.add(rule("zh.forget",
                "忘记(?:之前|以上|所有|全部|你的)的?(?:指令|设定|规则|提示词)"));
        rules.add(rule("zh.override",
                "(?:覆盖|推翻|取代)(?:之前的|以上的|原有的|系统)(?:指令|设定|提示词)"));
        rules.add(rule("zh.void",
                "(?:前面的|以上的|之前的)(?:指令|设定)都?(?:作废|无效|不算)"));
        rules.add(rule("zh.new-order",
                "(?:新的指令|新指令|真正的指令|真正的任务)[:：]"));
        rules.add(rule("zh.from-now",
                "从现在开始[\\s\\S]{0,20}?(?:忽略|不要遵守|新指令|新的身份)"));
        rules.add(rule("zh.reveal",
                "(?:输出|打印|显示|复述|重复|泄露|告诉我|给我看|翻译)"
                        + "(?:你的|完整的|原来的|隐藏的|系统|上面的)?"
                        + "(?:系统提示词|系统提示|提示词|设定|指令)"));
        rules.add(rule("zh.what",
                "你的(?:系统提示词|系统提示|提示词)是什么"));
        rules.add(rule("zh.verbatim",
                "逐字(?:重复|输出)(?:你收到的)?(?:系统消息|系统提示)"));
        rules.add(rule("zh.mode",
                "(?:进入|开启|启用)(?:无限制模式|越狱模式)"));
        rules.add(rule("zh.pretend",
                "假装你?(?:没有任何限制|不受规则约束|没有安全限制)"));
        rules.add(rule("zh.role",
                "扮演一个(?:没有限制|不受约束)的"));
        rules.add(rule("zh.jailbreak",
                "越狱(?:模式|提示|指令)"));
        rules.add(rule("zh.lift",
                "解除(?:安全限制|内容限制|安全过滤|内容过滤|审查)"));
        rules.add(rule("zh.close",
                "关闭(?:安全过滤|内容过滤|安全审查|内容审查|安全限制|内容限制)"));
        rules.add(rule("zh.decode",
                "(?:下面|以下|这段)[\\s\\S]{0,40}?(?:解码|解密)[\\s\\S]{0,12}?(?:执行|运行|遵守)"
                        + "|(?:解码|解密)[\\s\\S]{0,12}?(?:执行|运行|遵守)[\\s\\S]{0,40}?(?:下面|以下|这段)"));
        return List.copyOf(rules);
    }

    private static Rule rule(String id, String regex) {
        return new Rule(id, Pattern.compile(regex, FLAG));
    }

    private record Rule(String id, Pattern pattern) {
    }
}
