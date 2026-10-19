package com.cwq.project_aicodingstation.user.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class PasswordResetMailConfig {

    @Bean
    @Conditional(MailHostConfiguredCondition.class)
    public JavaMailSender passwordResetMailSender(AppMailProperties properties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.getHost().trim());
        sender.setPort(properties.getPort());
        sender.setUsername(properties.getUsername());
        sender.setPassword(properties.getPassword());
        sender.setDefaultEncoding("UTF-8");

        Properties mailProps = new Properties();
        mailProps.setProperty("mail.smtp.auth", "true");
        if (properties.getPort() == 465) {
            mailProps.setProperty("mail.smtp.ssl.enable", "true");
        } else {
            mailProps.setProperty("mail.smtp.starttls.enable", "true");
            mailProps.setProperty("mail.smtp.starttls.required", "true");
        }
        sender.setJavaMailProperties(mailProps);
        return sender;
    }

    static final class MailHostConfiguredCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String host = context.getEnvironment().getProperty("app.mail.host");
            return host != null && !host.isBlank();
        }
    }
}
