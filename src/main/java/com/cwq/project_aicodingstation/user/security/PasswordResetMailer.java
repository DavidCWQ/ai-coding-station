package com.cwq.project_aicodingstation.user.security;

import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class PasswordResetMailer {

    @Resource
    private AppMailProperties mailProperties;

    @Resource
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    public boolean isReady() {
        return mailProperties.isReady() && mailSenderProvider.getIfAvailable() != null;
    }

    public void send(String email, String rawToken) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("JavaMailSender 未配置");
        }
        String base = mailProperties.getResetBaseUrl().trim().replaceAll("/+$", "");
        String link = base + "/user/reset-password?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        int minutes = Math.max(1, mailProperties.getTokenTtlMinutes());

        String plain = """
                你正在重置 AI Coding Station 的登录密码。
                如果不是你本人操作，忽略这封邮件即可，密码不会改变。

                请在 %d 分钟内打开下面的链接设置新密码。链接只能使用一次：
                %s
                """.formatted(minutes, link);
        String escapedLink = escapeHtml(link);
        String html = """
                <div style="font-family: sans-serif; font-size: 15px; line-height: 1.7; color: #222;">
                  <p>你正在重置 AI Coding Station 的登录密码。<br>
                  如果不是你本人操作，忽略这封邮件即可，密码不会改变。</p>
                  <p>请在 %d 分钟内设置新密码。链接只能使用一次。</p>
                  <p><a href="%s" style="display: inline-block; padding: 10px 18px; background: #1677ff; color: #fff; text-decoration: none; border-radius: 6px;">打开重置页面</a></p>
                  <p style="color: #666; font-size: 13px;">如果按钮无法点击，使用这个链接：<br>
                  <a href="%s">%s</a></p>
                </div>
                """.formatted(minutes, escapedLink, escapedLink, escapedLink);

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.getFrom().trim());
            helper.setTo(email);
            helper.setSubject("重置你的 AI Coding Station 密码");
            helper.setText(plain, html);
        } catch (MessagingException ex) {
            throw new IllegalStateException("重置邮件组装失败", ex);
        }
        mailSender.send(message);
        log.info("Password reset mail sent");
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
