package com.example.WebRecon.recon.step;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Headers;

public class HeadersAuditor {

    public static class HeaderGrade {
        public String grade;
        public Map<String, Boolean> headerPresent = new LinkedHashMap<>();
        public List<String> issues = new ArrayList<>();
    }

    private static final String[] REQUIRED_HEADERS = {
        "Content-Security-Policy",
        "Strict-Transport-Security",
        "X-Frame-Options",
        "X-Content-Type-Options",
        "Referrer-Policy",
        "Permissions-Policy"
    };

    public static HeaderGrade audit(Headers headers) {
        HeaderGrade result = new HeaderGrade();
        int presentCount = 0;

        for (String name : REQUIRED_HEADERS) {
            String value = headers.get(name);
            boolean present = value != null && !value.isEmpty();
            result.headerPresent.put(name, present);
            if (present) {
                presentCount++;
                checkQuality(name, value, result);
            } else {
                result.issues.add("Missing: " + name);
            }
        }

        if (presentCount == 6) result.grade = "A";
        else if (presentCount == 5) result.grade = "B";
        else if (presentCount == 4) result.grade = "C";
        else if (presentCount == 3) result.grade = "D";
        else result.grade = "F";

        return result;
    }

    private static void checkQuality(String name, String value, HeaderGrade result) {
        if ("Strict-Transport-Security".equals(name)) {
            // Check max-age >= 15768000 (6 months)
            try {
                String[] parts = value.split(";");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("max-age=")) {
                        long maxAge = Long.parseLong(part.substring(8).trim());
                        if (maxAge < 15768000) {
                            result.issues.add("HSTS max-age too low (" + maxAge + " < 15768000)");
                        }
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
        if ("X-Frame-Options".equals(name)) {
            if (value.equalsIgnoreCase("ALLOWALL")) {
                result.issues.add("X-Frame-Options: ALLOWALL is insecure");
            }
        }
        if ("Content-Security-Policy".equals(name)) {
            if (value.contains("unsafe-inline") || value.contains("unsafe-eval")) {
                result.issues.add("CSP contains unsafe-inline or unsafe-eval");
            }
        }
    }

    public static String formatGradeReport(HeaderGrade grade) {
        StringBuilder sb = new StringBuilder();
        sb.append("Grade: ").append(grade.grade).append("\n\n");
        for (Map.Entry<String, Boolean> e : grade.headerPresent.entrySet()) {
            sb.append(e.getValue() ? "✓ " : "✗ ").append(e.getKey()).append("\n");
        }
        if (!grade.issues.isEmpty()) {
            sb.append("\nIssues:\n");
            for (String issue : grade.issues) {
                sb.append("• ").append(issue).append("\n");
            }
        }
        return sb.toString();
    }
}
