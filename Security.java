package com.hms.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import javax.servlet.http.HttpSession;

public class Security {

    private Security() { }

    /** SHA-256 hex digest of the given text. */
    public static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Returns the CSRF token for this session, creating one when needed. */
    public static String token(HttpSession session) {
        String t = (String) session.getAttribute("csrf");
        if (t == null) {
            t = UUID.randomUUID().toString();
            session.setAttribute("csrf", t);
        }
        return t;
    }
}
