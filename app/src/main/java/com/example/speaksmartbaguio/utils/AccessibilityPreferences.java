package com.example.speaksmartbaguio.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;

import java.util.WeakHashMap;

/**
 * Speak Smart Baguio
 *
 * Accessibility and Readability Preferences
 *
 * Features:
 * - Adjustable text size
 * - High contrast mode
 * - Simplified card layout
 * - Local preference storage using SharedPreferences
 *
 * No Room or Firestore changes are required.
 */
public final class AccessibilityPreferences {

    private static final String PREFS_NAME =
            "SpeakSmartAccessibilityPrefs";

    private static final String KEY_FONT_SCALE =
            "font_size_scale";

    private static final String KEY_HIGH_CONTRAST =
            "high_contrast";

    private static final String KEY_SIMPLIFIED_LAYOUT =
            "simplified_layout";

    public static final float FONT_SCALE_STANDARD = 1.0f;
    public static final float FONT_SCALE_LARGE = 1.2f;
    public static final float FONT_SCALE_EXTRA_LARGE = 1.4f;

    private static final float SIMPLIFIED_CARD_RADIUS_DP = 12f;

    /*
     * Original appearance caches prevent repeated application
     * from multiplying text sizes or permanently changing colors.
     */
    private static final WeakHashMap<TextView, Float>
            originalTextSizes = new WeakHashMap<>();

    private static final WeakHashMap<TextView, ColorStateList>
            originalTextColors = new WeakHashMap<>();

    private static final WeakHashMap<EditText, ColorStateList>
            originalHintColors = new WeakHashMap<>();

    private static final WeakHashMap<MaterialCardView, ColorStateList>
            originalCardColors = new WeakHashMap<>();

    private static final WeakHashMap<MaterialCardView, ColorStateList>
            originalStrokeColors = new WeakHashMap<>();

    private static final WeakHashMap<MaterialCardView, Float>
            originalCardRadii = new WeakHashMap<>();

    private static final WeakHashMap<MaterialCardView, Float>
            originalCardElevations = new WeakHashMap<>();

    private AccessibilityPreferences() {
        // Utility class; do not instantiate.
    }

    @NonNull
    private static SharedPreferences getPreferences(
            @NonNull Context context
    ) {
        return context.getApplicationContext()
                .getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                );
    }

    // ---------------------------------------------------------
    // FONT SIZE
    // ---------------------------------------------------------

    public static float getFontScale(
            @NonNull Context context
    ) {
        float scale = getPreferences(context).getFloat(
                KEY_FONT_SCALE,
                FONT_SCALE_STANDARD
        );

        if (scale != FONT_SCALE_STANDARD
                && scale != FONT_SCALE_LARGE
                && scale != FONT_SCALE_EXTRA_LARGE) {
            return FONT_SCALE_STANDARD;
        }

        return scale;
    }

    public static void setFontScale(
            @NonNull Context context,
            float scale
    ) {
        if (scale != FONT_SCALE_STANDARD
                && scale != FONT_SCALE_LARGE
                && scale != FONT_SCALE_EXTRA_LARGE) {
            scale = FONT_SCALE_STANDARD;
        }

        getPreferences(context)
                .edit()
                .putFloat(KEY_FONT_SCALE, scale)
                .apply();
    }

    // ---------------------------------------------------------
    // HIGH CONTRAST
    // ---------------------------------------------------------

    public static boolean isHighContrastEnabled(
            @NonNull Context context
    ) {
        return getPreferences(context).getBoolean(
                KEY_HIGH_CONTRAST,
                false
        );
    }

    public static void setHighContrastEnabled(
            @NonNull Context context,
            boolean enabled
    ) {
        getPreferences(context)
                .edit()
                .putBoolean(KEY_HIGH_CONTRAST, enabled)
                .apply();
    }

    // ---------------------------------------------------------
    // SIMPLIFIED LAYOUT
    // ---------------------------------------------------------

    public static boolean isSimplifiedLayoutEnabled(
            @NonNull Context context
    ) {
        return getPreferences(context).getBoolean(
                KEY_SIMPLIFIED_LAYOUT,
                false
        );
    }

    public static void setSimplifiedLayoutEnabled(
            @NonNull Context context,
            boolean enabled
    ) {
        getPreferences(context)
                .edit()
                .putBoolean(KEY_SIMPLIFIED_LAYOUT, enabled)
                .apply();
    }

    // ---------------------------------------------------------
    // RESET SETTINGS
    // ---------------------------------------------------------

    public static void reset(
            @NonNull Context context
    ) {
        getPreferences(context)
                .edit()
                .clear()
                .apply();
    }

    // ---------------------------------------------------------
    // APPLY SETTINGS TO A VIEW TREE
    // ---------------------------------------------------------

    /**
     * Applies the saved accessibility preferences to a view
     * and all of its descendants.
     *
     * Call this after inflating a screen or whenever saved
     * accessibility settings need to be reapplied.
     *
     * RecyclerView adapters should also call this method
     * for newly created item views.
     */
    public static void applyToViewTree(
            @NonNull Context context,
            @Nullable View root
    ) {
        if (root == null) {
            return;
        }

        float fontScale = getFontScale(context);
        boolean highContrast = isHighContrastEnabled(context);
        boolean simplified = isSimplifiedLayoutEnabled(context);

        applyRecursively(
                context,
                root,
                fontScale,
                highContrast,
                simplified
        );
    }

    private static void applyRecursively(
            @NonNull Context context,
            @NonNull View view,
            float fontScale,
            boolean highContrast,
            boolean simplified
    ) {
        applyToSingleView(
                context,
                view,
                fontScale,
                highContrast,
                simplified
        );

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                applyRecursively(
                        context,
                        group.getChildAt(i),
                        fontScale,
                        highContrast,
                        simplified
                );
            }
        }
    }

    // ---------------------------------------------------------
    // APPLY SETTINGS TO ONE VIEW
    // ---------------------------------------------------------

    private static void applyToSingleView(
            @NonNull Context context,
            @NonNull View view,
            float fontScale,
            boolean highContrast,
            boolean simplified
    ) {
        applyTextSettings(view, fontScale, highContrast);
        applyCardSettings(context, view, highContrast, simplified);
    }

    // ---------------------------------------------------------
    // TEXT SIZE, TEXT COLOR, AND EDIT TEXT HINTS
    // ---------------------------------------------------------

    private static void applyTextSettings(
            @NonNull View view,
            float fontScale,
            boolean highContrast
    ) {
        if (!(view instanceof TextView)) {
            return;
        }

        TextView textView = (TextView) view;

        /*
         * Cache the original pixel size once.
         * Always scale from that value to prevent compounding.
         */
        if (!originalTextSizes.containsKey(textView)) {
            originalTextSizes.put(
                    textView,
                    textView.getTextSize()
            );
        }

        Float originalSize = originalTextSizes.get(textView);

        if (originalSize != null) {
            textView.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    originalSize * fontScale
            );
        }

        /*
         * Cache the original text color once.
         */
        if (!originalTextColors.containsKey(textView)) {
            originalTextColors.put(
                    textView,
                    textView.getTextColors()
            );
        }

        ColorStateList originalColor =
                originalTextColors.get(textView);

        /*
         * High contrast uses white text for visibility.
         * Disabling high contrast restores the original color.
         */
        if (highContrast) {
            textView.setTextColor(Color.WHITE);
        } else if (originalColor != null) {
            textView.setTextColor(originalColor);
        }

        /*
         * Handle EditText hint colors separately.
         */
        if (textView instanceof EditText) {
            EditText editText = (EditText) textView;

            if (!originalHintColors.containsKey(editText)) {
                originalHintColors.put(
                        editText,
                        editText.getHintTextColors()
                );
            }

            if (highContrast) {
                editText.setHintTextColor(
                        ColorStateList.valueOf(Color.LTGRAY)
                );
            } else {
                ColorStateList originalHintColor =
                        originalHintColors.get(editText);

                if (originalHintColor != null) {
                    editText.setHintTextColor(
                            originalHintColor
                    );
                }
            }
        }
    }

    // ---------------------------------------------------------
    // MATERIAL CARD APPEARANCE
    // ---------------------------------------------------------

    private static void applyCardSettings(
            @NonNull Context context,
            @NonNull View view,
            boolean highContrast,
            boolean simplified
    ) {
        if (!(view instanceof MaterialCardView)) {
            return;
        }

        MaterialCardView card = (MaterialCardView) view;

        /*
         * Cache the original background color.
         */
        if (!originalCardColors.containsKey(card)) {
            originalCardColors.put(
                    card,
                    card.getCardBackgroundColor()
            );
        }

        /*
         * Cache the original border color.
         */
        if (!originalStrokeColors.containsKey(card)) {
            ColorStateList originalStroke =
                    card.getStrokeColorStateList();

            if (originalStroke == null) {
                originalStroke = ColorStateList.valueOf(
                        card.getStrokeColor()
                );
            }

            originalStrokeColors.put(card, originalStroke);
        }

        /*
         * Cache original corner radius and elevation.
         */
        if (!originalCardRadii.containsKey(card)) {
            originalCardRadii.put(
                    card,
                    card.getRadius()
            );
        }

        if (!originalCardElevations.containsKey(card)) {
            originalCardElevations.put(
                    card,
                    card.getCardElevation()
            );
        }

        // HIGH CONTRAST

        if (highContrast) {
            card.setCardBackgroundColor(Color.BLACK);
            card.setStrokeColor(Color.WHITE);
        } else {
            ColorStateList originalBackground =
                    originalCardColors.get(card);

            ColorStateList originalStroke =
                    originalStrokeColors.get(card);

            if (originalBackground != null) {
                card.setCardBackgroundColor(
                        originalBackground
                );
            }

            if (originalStroke != null) {
                card.setStrokeColor(originalStroke);
            }
        }

        // SIMPLIFIED LAYOUT

        if (simplified) {
            float radiusPx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    SIMPLIFIED_CARD_RADIUS_DP,
                    context.getResources().getDisplayMetrics()
            );

            card.setRadius(radiusPx);
            card.setCardElevation(0f);
        } else {
            Float originalRadius =
                    originalCardRadii.get(card);

            Float originalElevation =
                    originalCardElevations.get(card);

            if (originalRadius != null) {
                card.setRadius(originalRadius);
            }

            if (originalElevation != null) {
                card.setCardElevation(originalElevation);
            }
        }
    }
}
