package com.cwq.project_aicodingstation.user.security;

import java.util.Locale;
import java.util.regex.Pattern;

public final class EmailAddresses {

    private static final Pattern PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private EmailAddresses() {
    }

    /**
     * @return 规范化后的邮箱；格式不合法时返回 null
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String email = raw.trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty() || email.length() > 256 || !PATTERN.matcher(email).matches()) {
            return null;
        }
        return email;
    }
}
