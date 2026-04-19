package com.example.WebRecon.recon.step;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TechFingerprintStep {

    private static final String TAG = "TechFingerprint";

    public static class TechFingerprint {
        public List<String> techs = new ArrayList<>();
        public String server = "";
        public String poweredBy = "";
    }

    private static final String[][] COOKIE_RULES = {
        {"PHPSESSID", "PHP"},
        {"laravel_session", "Laravel"},
        {"wordpress_", "WordPress"},
        {"JSESSIONID", "Java/Servlet"},
        {"ASP.NET_SessionId", "ASP.NET"},
        {"rack.session", "Ruby/Rack"},
        {"django_", "Django"},
    };

    private static final String[][] SERVER_RULES = {
        {"nginx", "nginx"},
        {"apache", "Apache"},
        {"cloudflare", "Cloudflare"},
        {"express", "Express.js"},
        {"gunicorn", "Django/Python"},
        {"iis", "IIS"},
        {"litespeed", "LiteSpeed"},
    };

    private static final Pattern META_GENERATOR = Pattern.compile(
        "<meta[^>]+name=[\"']generator[\"'][^>]+content=[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );

    public static TechFingerprint fingerprint(String domain) {
        TechFingerprint result = new TechFingerprint();
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

        String url = "https://" + domain;
        try {
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();

            // Server header
            String server = response.header("Server", "");
            if (server != null && !server.isEmpty()) {
                result.server = server;
                for (String[] rule : SERVER_RULES) {
                    if (server.toLowerCase().contains(rule[0])) {
                        addIfAbsent(result.techs, rule[1]);
                    }
                }
            }

            // X-Powered-By
            String poweredBy = response.header("X-Powered-By", "");
            if (poweredBy != null && !poweredBy.isEmpty()) {
                result.poweredBy = poweredBy;
                addIfAbsent(result.techs, poweredBy);
            }

            // Cookies
            List<String> cookies = response.headers("Set-Cookie");
            for (String cookie : cookies) {
                for (String[] rule : COOKIE_RULES) {
                    if (cookie.contains(rule[0])) {
                        addIfAbsent(result.techs, rule[1]);
                    }
                }
            }

            // Meta generator in body
            if (response.body() != null) {
                String bodySnippet = "";
                try {
                    String full = response.body().string();
                    bodySnippet = full.length() > 20000 ? full.substring(0, 20000) : full;
                } catch (Exception ignored) {}
                Matcher m = META_GENERATOR.matcher(bodySnippet);
                if (m.find()) {
                    addIfAbsent(result.techs, m.group(1));
                }
                // WordPress extra check
                if (bodySnippet.contains("/wp-content/") || bodySnippet.contains("/wp-includes/")) {
                    addIfAbsent(result.techs, "WordPress");
                }
                // Cloudflare JS
                if (bodySnippet.contains("cloudflare")) {
                    addIfAbsent(result.techs, "Cloudflare");
                }
            }

            response.close();
        } catch (IOException e) {
            Log.w(TAG, "Fingerprint failed for " + domain + ": " + e.getMessage());
        }
        return result;
    }

    private static void addIfAbsent(List<String> list, String val) {
        for (String s : list) {
            if (s.equalsIgnoreCase(val)) return;
        }
        list.add(val);
    }
}
