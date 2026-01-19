package com.gateway.util;

import java.security.SecureRandom;

public final class IdUtil {
    private static final SecureRandom RND = new SecureRandom();
    private static final char[] ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private IdUtil() {}

    public static String randomId(String prefix, int len) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < len; i++) {
            sb.append(ALPHANUM[RND.nextInt(ALPHANUM.length)]);
        }
        return sb.toString();
    }
}
