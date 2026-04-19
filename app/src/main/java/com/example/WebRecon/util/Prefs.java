package com.example.WebRecon.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class Prefs {

    public static String getTheme(Context ctx) {
        return get(ctx).getString("theme", "system");
    }

    public static int getTimeoutSeconds(Context ctx) {
        String val = get(ctx).getString("request_timeout_seconds", "15");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 15;
        }
    }

    public static String getUserAgent(Context ctx) {
        return get(ctx).getString("user_agent", "WebReconToolkit/1.0");
    }

    public static boolean isHibpEnabled(Context ctx) {
        return get(ctx).getBoolean("hibp_enabled", true);
    }

    public static String getReconIntensity(Context ctx) {
        return get(ctx).getString("recon_intensity", "medium");
    }

    public static boolean isWordlistsCopied(Context ctx) {
        return get(ctx).getBoolean("wordlists_copied", false);
    }

    public static void setWordlistsCopied(Context ctx, boolean value) {
        get(ctx).edit().putBoolean("wordlists_copied", value).apply();
    }

    private static SharedPreferences get(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }
}
