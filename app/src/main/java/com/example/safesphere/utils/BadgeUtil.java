
package com.example.safesphere.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.example.safesphere.R;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BadgeUtil {

    private static final int BADGE_VIEW_ID = 999999;  // Unique ID for badge

    // 🔹 Attach badge on any view (Button, ImageView, AppCompatButton etc.)
    public static void attachBadge(Context context, View targetView, int count) {

        ViewGroup parent = (ViewGroup) targetView.getParent();

        // Parent FrameLayout nahi hai → convert karo
        if (!(parent instanceof FrameLayout)) {
            FrameLayout frameLayout = new FrameLayout(context);

            int index = parent.indexOfChild(targetView);
            parent.removeView(targetView);

            frameLayout.addView(targetView);
            parent.addView(frameLayout, index);

            parent = frameLayout; // New parent
        }

        // Agar count = 0 → badge clear
        if (count == 0) {
            clearBadge(targetView);
            return;
        }

        // Pehle existing badge hatao (duplicate avoid)
        View oldBadge = parent.findViewById(BADGE_VIEW_ID);
        if (oldBadge != null) parent.removeView(oldBadge);

        // New Badge
        TextView badge = new TextView(context, null, 0, R.style.NotificationBadge);
        badge.setId(BADGE_VIEW_ID);
        badge.setText(String.valueOf(count));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.END | Gravity.TOP;
        badge.setLayoutParams(params);

        parent.addView(badge);
    }

    // 🔹 Clear badge from any view
    public static void clearBadge(View targetView) {
        ViewGroup parent = (ViewGroup) targetView.getParent();

        if (parent instanceof FrameLayout) {
            View badge = parent.findViewById(BADGE_VIEW_ID);
            if (badge != null) parent.removeView(badge);
        }
    }

    // 🔹 BottomNavigationView badge (already good)
    public static void setBadge(Context context, BottomNavigationView bottomNavigationView,
                                @IdRes int menuItemId, int count) {

        BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(menuItemId);

        badge.setVisible(count > 0);
        badge.setNumber(count);

        badge.setBackgroundColor(ContextCompat.getColor(context, R.color.red));
        badge.setBadgeTextColor(ContextCompat.getColor(context, android.R.color.white));
        
    }

}
