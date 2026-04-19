package com.example.WebRecon.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WordlistManager {

    private static final String TAG = "WordlistManager";

    public static final String FILE_SUBDOMAINS = "subdomains_common.txt";
    public static final String FILE_PATHS = "sensitive_paths.txt";
    public static final String FILE_JWT_SECRETS = "weak_jwt_secrets.txt";

    public static void copyAssetsToInternalStorage(Context ctx) {
        if (Prefs.isWordlistsCopied(ctx)) return;
        try {
            copyAsset(ctx, FILE_SUBDOMAINS);
            copyAsset(ctx, FILE_PATHS);
            copyAsset(ctx, FILE_JWT_SECRETS);
            Prefs.setWordlistsCopied(ctx, true);
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy wordlists", e);
        }
    }

    private static void copyAsset(Context ctx, String filename) throws IOException {
        File dest = new File(ctx.getFilesDir(), filename);
        try (InputStream in = ctx.getAssets().open(filename);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
    }

    public static List<String> readLines(Context ctx, String filename) {
        List<String> lines = new ArrayList<>();
        File file = new File(ctx.getFilesDir(), filename);
        if (!file.exists()) {
            // Fall back to reading directly from assets
            try (InputStream in = ctx.getAssets().open(filename);
                 BufferedReader reader = new BufferedReader(
                     new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        lines.add(line);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to read asset " + filename, e);
            }
            return lines;
        }
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new java.io.FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read " + filename, e);
        }
        return lines;
    }
}
