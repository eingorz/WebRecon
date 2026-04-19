package com.example.WebRecon.tools;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class JwtTool {

    public static class JwtDecodeResult {
        public String headerJson;
        public String payloadJson;
        public String signatureB64;
        public List<String> issues = new ArrayList<>();
        public boolean success;
        public String error;
    }

    public static JwtDecodeResult decode(String token) {
        JwtDecodeResult result = new JwtDecodeResult();
        if (token == null || token.isBlank()) {
            result.error = "Empty token";
            return result;
        }

        String[] parts = token.trim().split("\\.");
        if (parts.length < 2) {
            result.error = "Invalid JWT: must have at least 2 parts";
            return result;
        }

        try {
            result.headerJson = prettyJson(base64UrlDecode(parts[0]));
            result.payloadJson = prettyJson(base64UrlDecode(parts[1]));
            result.signatureB64 = parts.length > 2 ? parts[2] : "(none)";
            result.success = true;

            checkIssues(result, parts);
        } catch (Exception e) {
            result.error = "Decode failed: " + e.getMessage();
        }
        return result;
    }

    private static void checkIssues(JwtDecodeResult result, String[] parts) {
        try {
            JSONObject header = new JSONObject(result.headerJson);
            JSONObject payload = new JSONObject(result.payloadJson);

            // alg=none
            String alg = header.optString("alg", "");
            if ("none".equalsIgnoreCase(alg)) {
                result.issues.add("CRITICAL: alg=none — signature not verified");
            }

            // Missing exp
            if (!payload.has("exp")) {
                result.issues.add("WARNING: Missing 'exp' claim — token never expires");
            } else {
                long exp = payload.getLong("exp");
                if (exp < System.currentTimeMillis() / 1000L) {
                    result.issues.add("WARNING: Token is expired (exp=" + exp + ")");
                }
            }

            // Missing iat
            if (!payload.has("iat")) {
                result.issues.add("INFO: Missing 'iat' claim");
            }

            // HS256 weak secret — brute force is done separately via bruteSecret()
            if ("HS256".equals(alg) && parts.length == 3) {
                result.issues.add("INFO: HS256 — use 'Check Weak Secrets' to test common passwords");
            }

        } catch (JSONException e) {
            result.issues.add("Could not parse claims: " + e.getMessage());
        }
    }

    /**
     * Attempts to find which secret from the list was used to sign the token.
     * Returns the matching secret, or null if none matched.
     */
    public static String bruteSecret(String token, List<String> secrets) {
        String[] parts = token.trim().split("\\.");
        if (parts.length != 3) return null;

        String headerPayload = parts[0] + "." + parts[1];
        String actualSig = parts[2];

        for (String secret : secrets) {
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                byte[] sigBytes = mac.doFinal(headerPayload.getBytes(StandardCharsets.UTF_8));
                String computed = Base64.encodeToString(sigBytes, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
                if (computed.equals(actualSig)) {
                    return secret;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static String base64UrlDecode(String s) {
        // Pad to multiple of 4
        switch (s.length() % 4) {
            case 2: s += "=="; break;
            case 3: s += "="; break;
            default: break;
        }
        byte[] decoded = Base64.decode(s, Base64.URL_SAFE | Base64.NO_WRAP);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private static String prettyJson(String raw) {
        try {
            return new JSONObject(raw).toString(2);
        } catch (JSONException e) {
            return raw;
        }
    }
}
