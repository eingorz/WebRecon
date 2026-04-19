package com.example.WebRecon.tools;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HibpChecker {

    public interface HibpResult {
        void onResult(int count);
        void onError(String message);
    }

    public static void checkPwned(String password, OkHttpClient client, HibpResult callback) {
        String prefix = HashTool.sha1Prefix(password);
        String suffix = HashTool.sha1Suffix(password);

        Request request = new Request.Builder()
            .url("https://api.pwnedpasswords.com/range/" + prefix)
            .header("Add-Padding", "true")
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("HIBP returned " + response.code());
                    return;
                }
                String body = response.body() != null ? response.body().string() : "";
                int count = findCount(body, suffix);
                callback.onResult(count);
            }
        });
    }

    private static int findCount(String body, String suffix) {
        for (String line : body.split("\n")) {
            String[] parts = line.trim().split(":");
            if (parts.length == 2 && parts[0].equalsIgnoreCase(suffix)) {
                try {
                    return Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }
}
