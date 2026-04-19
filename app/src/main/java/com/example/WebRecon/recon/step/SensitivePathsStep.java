package com.example.WebRecon.recon.step;

import android.content.Context;

import com.example.WebRecon.db.FindingType;
import com.example.WebRecon.db.Severity;
import com.example.WebRecon.db.entity.Finding;
import com.example.WebRecon.util.Prefs;
import com.example.WebRecon.util.WordlistManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SensitivePathsStep {

    private static final String TAG = "SensitivePaths";
    private static final int POOL_SIZE = 10;

    public static List<Finding> check(Context context, String domain, long engagementId) {
        List<String> paths = WordlistManager.readLines(context, WordlistManager.FILE_PATHS);

        // Filter by intensity
        String intensity = Prefs.getReconIntensity(context);
        if ("small".equals(intensity) && paths.size() > 50) {
            paths = paths.subList(0, 50);
        }

        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build();

        List<Finding> findings = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);
        CountDownLatch latch = new CountDownLatch(paths.size());
        Object lock = new Object();

        for (String path : paths) {
            final String finalPath = path;
            pool.submit(() -> {
                try {
                    String url = "https://" + domain + finalPath;
                    Request req = new Request.Builder().url(url).head().build();
                    Response resp = client.newCall(req).execute();
                    int code = resp.code();
                    resp.close();

                    if (code == 200 || code == 401 || code == 403) {
                        Severity sev = code == 200 ? Severity.CRIT : Severity.WARN;
                        Finding f = new Finding(engagementId, FindingType.PATH, sev,
                            "HTTP " + code + " → " + finalPath,
                            "Path " + finalPath + " returned " + code);
                        synchronized (lock) {
                            findings.add(f);
                        }
                    }
                } catch (IOException e) {
                    // Network error for this path — skip silently
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pool.shutdown();
        return findings;
    }
}
