package com.example.WebRecon.tools;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class EncoderTool {

    // --- Base64 ---

    public static String encodeBase64(String input) {
        return Base64.encodeToString(input.getBytes(StandardCharsets.UTF_8),
            Base64.NO_WRAP);
    }

    public static String decodeBase64(String input) throws IllegalArgumentException {
        byte[] decoded = Base64.decode(input.trim(), Base64.DEFAULT);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    // --- URL ---

    public static String encodeUrl(String input) {
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return input; // never happens with UTF-8
        }
    }

    public static String decodeUrl(String input) {
        try {
            return URLDecoder.decode(input, "UTF-8");
        } catch (Exception e) {
            return "Decode error: " + e.getMessage();
        }
    }

    // --- Hex ---

    public static String encodeHex(String input) {
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String decodeHex(String input) {
        input = input.trim().replaceAll("\\s", "");
        if (input.length() % 2 != 0) return "Invalid hex: odd length";
        try {
            byte[] bytes = new byte[input.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) Integer.parseInt(input.substring(i * 2, i * 2 + 2), 16);
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (NumberFormatException e) {
            return "Invalid hex: " + e.getMessage();
        }
    }

    // --- HTML entities ---

    public static String encodeHtml(String input) {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    public static String decodeHtml(String input) {
        return input
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ");
    }

    // --- Auto-detect ---

    public static String detectFormat(String input) {
        if (input == null || input.isEmpty()) return "unknown";
        String trimmed = input.trim();

        // HTML entities
        if (trimmed.contains("&amp;") || trimmed.contains("&lt;") || trimmed.contains("&gt;")
            || trimmed.contains("&quot;") || trimmed.contains("&#")) {
            return "html";
        }

        // URL encoded
        if (trimmed.matches(".*%[0-9A-Fa-f]{2}.*")) {
            return "url";
        }

        // Hex: all hex chars, even length, at least 4 chars
        if (trimmed.length() >= 4 && trimmed.length() % 2 == 0
            && trimmed.matches("[0-9a-fA-F]+")) {
            return "hex";
        }

        // Base64: valid base64 charset including padding
        if (trimmed.matches("[A-Za-z0-9+/=]+") && trimmed.length() % 4 == 0) {
            return "base64";
        }

        return "unknown";
    }

    public static String autoDecode(String input) {
        String format = detectFormat(input);
        switch (format) {
            case "html": return decodeHtml(input);
            case "url": return decodeUrl(input);
            case "hex": return decodeHex(input);
            case "base64": {
                try {
                    return decodeBase64(input);
                } catch (Exception e) {
                    return "Base64 decode failed: " + e.getMessage();
                }
            }
            default: return "Could not detect format";
        }
    }
}
