package com.example.safesphere.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

import com.example.safesphere.R;

public class ThemeUtils {

    private static final String PREF_NAME = "app_theme_pref";
    private static final String KEY_THEME = "selected_theme";

    public static void saveTheme(@NonNull Context context, @NonNull String themeName) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, themeName).apply();
    }

    public static void applyTheme(@NonNull Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String themeName = prefs.getString(KEY_THEME, "SafeSphere_dark");

        switch (themeName) {
            case "cyan":
                activity.setTheme(R.style.Theme_cyan);
                break;
            case "navy_blue":
                activity.setTheme(R.style.Theme_navy_blue);
                break;
            case "SafeSphere":
                activity.setTheme(R.style.Theme_SafeSphere);
                break;
            case "yellow":
                activity.setTheme(R.style.Theme_yellow);
                break;
            case "SafeSphere_dark":
                activity.setTheme(R.style.Theme_SafeSphere_dark);
                break;
            case "dark_pink":
                activity.setTheme(R.style.Theme_dark_pink);
                break;
            case "dark_orange":
                activity.setTheme(R.style.Theme_dark_orange);
                break;
            case "teal":
                activity.setTheme(R.style.Theme_teal);
                break;
            default:
                activity.setTheme(R.style.Theme_SafeSphere);
                break;
        }
    }


    public static void changeTheme(@NonNull Activity activity, @NonNull String themeName) {
        saveTheme(activity, themeName);
        applyTheme(activity);
        Intent intent = activity.getIntent();
        activity.finish();
        activity.overridePendingTransition(0, 0);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    public static boolean isDarkTheme(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String themeName = prefs.getString(KEY_THEME, "SafeSphere");

        return themeName.equals("SafeSphere_dark")
                || themeName.equals("dark_pink")
                || themeName.equals("dark_orange")
                || themeName.equals("cyan")
                || themeName.equals("yellow");
    }

}
