package com.example.WebRecon.recon.step;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CrtShStep {

    private static final String TAG = "CrtShStep";
    private static final int TIMEOUT_SEC = 60;

    public static List<String> enumerate(String domain) {
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
            .build();

        String url = "https://crt.sh/?q=" + domain + "&output=json";
        Request req = new Request.Builder().url(url).build();

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                Response response = client.newCall(req).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    continue;
                }
                String body = response.body().string();
                response.close();
                return parseSubdomains(body, domain);
            } catch (IOException e) {
                Log.w(TAG, "crt.sh attempt " + (attempt + 1) + " failed: " + e.getMessage());
            }
        }
        Log.w(TAG, "crt.sh failed — returning empty list");
        return new ArrayList<>();
    }

    private static List<String> parseSubdomains(String json, String domain) {
        Set<String> result = new HashSet<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String nameValue = obj.optString("name_value", "");
                for (String entry : nameValue.split("\n")) {
                    entry = entry.trim().toLowerCase();
                    // Strip wildcard prefix
                    if (entry.startsWith("*.")) entry = entry.substring(2);
                    if (entry.endsWith("." + domain) || entry.equals(domain)) {
                        result.add(entry);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "JSON parse error", e);
        }
        return new ArrayList<>(result);
    }
}
