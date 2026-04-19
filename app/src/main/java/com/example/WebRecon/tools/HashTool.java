package com.example.WebRecon.tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class HashTool {

    public static String hash(String input, String algo) {
        try {
            MessageDigest md = MessageDigest.getInstance(algo);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return "Unsupported algorithm: " + algo;
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Identifies a hash algorithm from its hex string based on length.
     */
    public static String identifyHash(String hexHash) {
        if (hexHash == null) return "Unknown";
        String h = hexHash.trim().toLowerCase();
        if (!h.matches("[0-9a-f]+")) return "Not a hex hash";
        switch (h.length()) {
            case 32: return "MD5 (32 chars)";
            case 40: return "SHA-1 (40 chars)";
            case 56: return "SHA-224 (56 chars)";
            case 64: return "SHA-256 (64 chars)";
            case 96: return "SHA-384 (96 chars)";
            case 128: return "SHA-512 (128 chars)";
            default: return "Unknown (" + h.length() + " chars)";
        }
    }

    /** Returns the SHA-1 hash prefix (5 chars uppercase) for HIBP range API. */
    public static String sha1Prefix(String input) {
        String sha1 = hash(input, "SHA-1").toUpperCase();
        return sha1.length() >= 5 ? sha1.substring(0, 5) : sha1;
    }

    /** Returns the SHA-1 hash suffix (after first 5 chars) for HIBP range response lookup. */
    public static String sha1Suffix(String input) {
        String sha1 = hash(input, "SHA-1").toUpperCase();
        return sha1.length() > 5 ? sha1.substring(5) : "";
    }
}
