package com.example.WebRecon.net;

import android.content.Context;

import com.example.WebRecon.util.Prefs;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class HttpClient {

    private static volatile OkHttpClient instance;

    public static OkHttpClient get(Context context) {
        if (instance == null) {
            synchronized (HttpClient.class) {
                if (instance == null) {
                    int timeout = Prefs.getTimeoutSeconds(context);
                    instance = build(timeout);
                }
            }
        }
        return instance;
    }

    public static OkHttpClient build(int timeoutSeconds) {
        return new OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .followRedirects(true)
            .build();
    }

    /** Call this when timeout setting changes. */
    public static void invalidate() {
        instance = null;
    }
}
