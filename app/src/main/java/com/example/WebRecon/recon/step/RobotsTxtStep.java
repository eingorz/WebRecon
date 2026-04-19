package com.example.WebRecon.recon.step;

import android.util.Log;

import com.example.WebRecon.db.FindingType;
import com.example.WebRecon.db.Severity;
import com.example.WebRecon.db.entity.Finding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RobotsTxtStep {

    private static final String TAG = "RobotsTxtStep";

    public static List<Finding> check(String domain, long engagementId) {
        List<Finding> findings = new ArrayList<>();
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

        // robots.txt
        checkUrl(client, "https://" + domain + "/robots.txt", domain, engagementId, findings);

        // sitemap.xml
        try {
            Request req = new Request.Builder()
                .url("https://" + domain + "/sitemap.xml")
                .head()
                .build();
            Response resp = client.newCall(req).execute();
            int code = resp.code();
            resp.close();
            if (code == 200) {
                findings.add(new Finding(engagementId, FindingType.ROBOTS, Severity.INFO,
                    "sitemap.xml found",
                    "https://" + domain + "/sitemap.xml exists (HTTP 200)"));
            }
        } catch (IOException e) {
            Log.d(TAG, "sitemap.xml check failed: " + e.getMessage());
        }

        return findings;
    }

    private static void checkUrl(OkHttpClient client, String url, String domain,
                                  long engagementId, List<Finding> findings) {
        try {
            Request req = new Request.Builder().url(url).build();
            Response resp = client.newCall(req).execute();
            int code = resp.code();

            if (code == 200 && resp.body() != null) {
                String body = resp.body().string();
                resp.close();

                findings.add(new Finding(engagementId, FindingType.ROBOTS, Severity.INFO,
                    "robots.txt found",
                    "robots.txt is accessible at " + url));

                List<String> disallowed = new ArrayList<>();
                for (String line : body.split("\n")) {
                    line = line.trim();
                    if (line.toLowerCase().startsWith("disallow:")) {
                        String path = line.substring(9).trim();
                        if (!path.isEmpty() && !path.equals("/")) {
                            disallowed.add(path);
                        }
                    }
                }
                if (!disallowed.isEmpty()) {
                    findings.add(new Finding(engagementId, FindingType.ROBOTS, Severity.INFO,
                        "Disallowed paths in robots.txt (" + disallowed.size() + ")",
                        String.join("\n", disallowed)));
                }
            } else {
                resp.close();
            }
        } catch (IOException e) {
            Log.d(TAG, "robots.txt check failed: " + e.getMessage());
        }
    }
}
