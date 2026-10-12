package com.example.speaksmartbaguio.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.speaksmartbaguio.R;

public final class SettingsMenuHelper {

    private SettingsMenuHelper() {
        // Prevent creating instances of this helper.
    }

    // =========================================================
    // COLORS — MATCH SPEAK SMART BAGUIO'S DARK AND TEAL DESIGN
    // =========================================================

    private static final int COLOR_BACKGROUND =
            Color.rgb(23, 26, 28);

    private static final int COLOR_BORDER =
            Color.rgb(52, 59, 61);

    private static final int COLOR_ICON_BACKGROUND =
            Color.rgb(18, 58, 55);

    private static final int COLOR_TEAL =
            Color.rgb(8, 212, 194);

    private static final int COLOR_TITLE =
            Color.rgb(255, 255, 255);

    private static final int COLOR_SUBTITLE =
            Color.rgb(174, 184, 185);

    private static final int COLOR_DIVIDER =
            Color.rgb(54, 61, 63);

    // =========================================================
    // MENU ITEM IDS
    // =========================================================

    private static final int MENU_ACCESSIBILITY = 1001;
    private static final int MENU_AUDIO = 1002;
    private static final int MENU_LANGUAGE = 1003;
    private static final int MENU_ABOUT = 1004;

    // =========================================================
    // SHOW SETTINGS POPUP
    // =========================================================

    public static void show(View anchor) {

        if (anchor == null || anchor.getContext() == null) {
            return;
        }

        Context context = anchor.getContext();

        // Main popup container.
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        container.setPadding(
                dp(context, 10),
                dp(context, 10),
                dp(context, 10),
                dp(context, 10)
        );

        container.setBackground(createRoundedBackground(
                COLOR_BACKGROUND,
                dp(context, 18),
                COLOR_BORDER,
                dp(context, 1)
        ));

        container.setClipToOutline(true);

        // Calculate popup width.
        int screenWidth = context.getResources()
                .getDisplayMetrics().widthPixels;

        int popupWidth = Math.min(
                dp(context, 320),
                screenWidth - dp(context, 24)
        );

        // Create the popup before adding rows.
        PopupWindow popupWindow = new PopupWindow(
                container,
                popupWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT)
        );

        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setElevation(dp(context, 12));

        // Accessibility & Readability.
        addMenuItem(
                context,
                container,
                anchor,
                popupWindow,
                MENU_ACCESSIBILITY,
                "♿",
                "Accessibility & Readability",
                "Text size and accessibility",
                true
        );

        // Audio & Pronunciation.
        addMenuItem(
                context,
                container,
                anchor,
                popupWindow,
                MENU_AUDIO,
                "♫",
                "Audio & Pronunciation",
                "Listen and practice speech",
                true
        );

        // Language Preferences.
        addMenuItem(
                context,
                container,
                anchor,
                popupWindow,
                MENU_LANGUAGE,
                "文",
                "Language Preferences",
                "Choose your preferred languages",
                true
        );

        // About Speak Smart.
        addMenuItem(
                context,
                container,
                anchor,
                popupWindow,
                MENU_ABOUT,
                "i",
                "About Speak Smart",
                "App information and details",
                false
        );

        // Display popup aligned with the gear button.
        if (anchor.isAttachedToWindow()) {
            popupWindow.showAsDropDown(
                    anchor,
                    0,
                    dp(context, 8),
                    Gravity.END
            );
        }
    }

    // =========================================================
    // CREATE EACH MENU ROW
    // =========================================================

    private static void addMenuItem(
            Context context,
            LinearLayout container,
            View originalAnchor,
            PopupWindow popupWindow,
            int menuId,
            String iconText,
            String title,
            String subtitle,
            boolean showDivider
    ) {

        // Entire row is clickable.
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        row.setPadding(
                dp(context, 8),
                dp(context, 11),
                dp(context, 8),
                dp(context, 11)
        );

        row.setBackground(createRoundedBackground(
                Color.TRANSPARENT,
                dp(context, 12),
                Color.TRANSPARENT,
                0
        ));

        row.setClickable(true);
        row.setFocusable(true);

        // Teal icon tile.
        TextView icon = new TextView(context);
        icon.setText(iconText);
        icon.setTextColor(COLOR_TEAL);
        icon.setTextSize(21);
        icon.setTypeface(Typeface.DEFAULT_BOLD);
        icon.setGravity(Gravity.CENTER);

        icon.setBackground(createRoundedBackground(
                COLOR_ICON_BACKGROUND,
                dp(context, 11),
                Color.TRANSPARENT,
                0
        ));

        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(
                        dp(context, 42),
                        dp(context, 42)
                );

        iconParams.setMarginEnd(dp(context, 12));
        row.addView(icon, iconParams);

        // Title and subtitle.
        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(COLOR_TITLE);
        titleView.setTextSize(14);
        titleView.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );
        titleView.setMaxLines(2);

        TextView subtitleView = new TextView(context);
        subtitleView.setText(subtitle);
        subtitleView.setTextColor(COLOR_SUBTITLE);
        subtitleView.setTextSize(11);
        subtitleView.setMaxLines(2);

        LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        subtitleParams.topMargin = dp(context, 3);

        textContainer.addView(titleView);
        textContainer.addView(subtitleView, subtitleParams);

        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                );

        row.addView(textContainer, textParams);

        // Right-facing chevron.
        TextView chevron = new TextView(context);
        chevron.setText("›");
        chevron.setTextColor(COLOR_TEAL);
        chevron.setTextSize(25);
        chevron.setGravity(Gravity.CENTER);

        row.addView(
                chevron,
                new LinearLayout.LayoutParams(
                        dp(context, 22),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // Dismiss the popup before handling the selection.
        row.setOnClickListener(v -> {

            if (popupWindow.isShowing()) {
                popupWindow.dismiss();
            }

            handleMenuSelection(originalAnchor, menuId);
        });

        container.addView(
                row,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // Separator between menu options.
        if (showDivider) {
            View divider = new View(context);
            divider.setBackgroundColor(COLOR_DIVIDER);

            LinearLayout.LayoutParams dividerParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(context, 1)
                    );

            dividerParams.setMargins(
                    dp(context, 8),
                    dp(context, 2),
                    dp(context, 8),
                    dp(context, 2)
            );

            container.addView(divider, dividerParams);
        }
    }

    // =========================================================
    // HANDLE MENU SELECTION
    // =========================================================

    private static void handleMenuSelection(
            View anchor,
            int menuId
    ) {

        if (anchor == null || anchor.getContext() == null) {
            return;
        }

        Context context = anchor.getContext();

        if (menuId == MENU_ACCESSIBILITY) {

            navigateToDestination(
                    anchor,
                    R.id.navigation_accessibility_settings,
                    "Unable to open accessibility settings."
            );

        } else if (menuId == MENU_AUDIO) {

            Toast.makeText(
                    context,
                    "Audio & Pronunciation — Coming Soon",
                    Toast.LENGTH_SHORT
            ).show();

        } else if (menuId == MENU_LANGUAGE) {

            Toast.makeText(
                    context,
                    "Language Preferences — Coming Soon",
                    Toast.LENGTH_SHORT
            ).show();

        } else if (menuId == MENU_ABOUT) {

            // NEW: Open the About screen instead of showing a toast.
            navigateToDestination(
                    anchor,
                    R.id.navigation_about,
                    "Unable to open About Speak Smart."
            );
        }
    }

    // =========================================================
    // NAVIGATION HELPER
    // =========================================================

    private static void navigateToDestination(
            View anchor,
            int destinationId,
            String errorMessage
    ) {

        Context context = anchor.getContext();

        try {
            NavController navController =
                    Navigation.findNavController(anchor);

            // Avoid adding the current destination to itself.
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId()
                    == destinationId) {
                return;
            }

            navController.navigate(destinationId);

        } catch (IllegalArgumentException
                 | IllegalStateException exception) {

            Toast.makeText(
                    context,
                    errorMessage,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // ROUNDED BACKGROUND HELPER
    // =========================================================

    private static GradientDrawable createRoundedBackground(
            int backgroundColor,
            int cornerRadius,
            int strokeColor,
            int strokeWidth
    ) {

        GradientDrawable drawable = new GradientDrawable();

        drawable.setColor(backgroundColor);
        drawable.setCornerRadius(cornerRadius);

        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }

        return drawable;
    }

    // =========================================================
    // DP TO PIXELS
    // =========================================================

    private static int dp(Context context, float value) {

        return (int) (
                value * context.getResources()
                        .getDisplayMetrics().density + 0.5f
        );
    }
}
