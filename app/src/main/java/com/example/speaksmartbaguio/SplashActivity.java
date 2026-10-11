package com.example.speaksmartbaguio;

import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.ybq.android.spinkit.SpinKitView;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.ThreeBounce;

public class SplashActivity extends AppCompatActivity {

    private static final String GROUP_NAME = "Taraki ti BP";

    // Total splash duration.
    private static final long SPLASH_DELAY = 6500L;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean navigationStarted = false;

    private final Runnable openHome = () -> {
        if (isFinishing() || isDestroyed() || navigationStarted) {
            return;
        }

        navigationStarted = true;

        Intent intent = new Intent(
                SplashActivity.this,
                HomeActivity.class
        );

        startActivity(intent);

        // Smoothly remove the splash screen.
        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );

        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        // ==========================================
        // BAGUIO FROST SYSTEM BARS
        // ==========================================

        getWindow().setStatusBarColor(
                ContextCompat.getColor(
                        this,
                        R.color.app_background
                )
        );

        getWindow().setNavigationBarColor(
                ContextCompat.getColor(
                        this,
                        R.color.app_background
                )
        );

        // ==========================================
        // CONNECT VIEWS
        // ==========================================

        ImageView splashLogo =
                findViewById(R.id.splashLogo);

        TextView textDeveloperCredit =
                findViewById(R.id.textDeveloperCredit);

        LinearLayout letterContainer =
                findViewById(R.id.letterContainer);

        TextView textGroupSub =
                findViewById(R.id.textGroupSub);

        SpinKitView spinnerDots =
                findViewById(R.id.spinnerDots);

        // ==========================================
        // TEAL GRADIENT COLORS
        // ==========================================

        int tealStart = ContextCompat.getColor(
                this,
                R.color.primary_teal
        );

        int tealEnd = ContextCompat.getColor(
                this,
                R.color.accent_teal
        );

        // ==========================================
        // SCENE 1: LOGO REVEAL
        // Fade in and gently scale up.
        // ==========================================

        splashLogo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(900)
                .start();

        // ==========================================
        // SCENE 2: DEVELOPER CREDIT
        // Fade in with a slight upward movement.
        // ==========================================

        textDeveloperCredit.setTranslationY(12f);

        textDeveloperCredit.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(750)
                .setDuration(800)
                .start();

        // ==========================================
        // SCENE 3: ANIMATED GROUP TITLE
        // Preserve the original group name.
        // ==========================================

        for (char c : GROUP_NAME.toCharArray()) {

            TextView letter = new TextView(this);

            letter.setText(String.valueOf(c));
            letter.setTextSize(36f);
            letter.setTypeface(null, Typeface.BOLD);
            letter.setAlpha(0f);

            letter.setTextColor(
                    ContextCompat.getColor(
                            this,
                            R.color.text_light
                    )
            );

            Shader shader = new LinearGradient(
                    0f,
                    0f,
                    0f,
                    48f,
                    tealStart,
                    tealEnd,
                    Shader.TileMode.CLAMP
            );

            letter.getPaint().setShader(shader);

            // Give each letter a little breathing room.
            letter.setPadding(1, 0, 1, 0);

            letterContainer.addView(letter);
        }

        // Start the title after the logo and credit
        // have had time to appear.
        handler.postDelayed(() -> {

            if (isFinishing() || isDestroyed()) {
                return;
            }

            animateLetters(
                    letterContainer,
                    0,
                    () -> revealTagline(
                            textGroupSub,
                            spinnerDots
                    )
            );

        }, 1600);

        // ==========================================
        // SPINNER STYLE
        // ==========================================

        Sprite threeBounce = new ThreeBounce();
        threeBounce.setColor(tealStart);

        spinnerDots.setIndeterminateDrawable(threeBounce);

        // ==========================================
        // FINAL TRANSITION
        // Open HomeActivity after the introduction.
        // ==========================================

        handler.postDelayed(openHome, SPLASH_DELAY);
    }

    // ==============================================
    // LETTER-BY-LETTER TITLE ANIMATION
    // ==============================================

    private void animateLetters(
            LinearLayout container,
            int index,
            Runnable onComplete
    ) {

        if (index >= container.getChildCount()) {
            onComplete.run();
            return;
        }

        TextView letter =
                (TextView) container.getChildAt(index);

        letter.setTranslationY(10f);

        letter.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(150)
                .withEndAction(() ->
                        animateLetters(
                                container,
                                index + 1,
                                onComplete
                        )
                )
                .start();
    }

    // ==============================================
    // SCENE 4: TAGLINE REVEAL
    // ==============================================

    private void revealTagline(
            TextView tagline,
            SpinKitView spinner
    ) {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        tagline.setTranslationY(8f);

        tagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(750)
                .withEndAction(() -> revealSpinner(spinner))
                .start();
    }

    // ==============================================
    // SCENE 5: LOADING INDICATOR
    // ==============================================

    private void revealSpinner(SpinKitView spinner) {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        spinner.animate()
                .alpha(1f)
                .setDuration(500)
                .start();
    }

    // ==============================================
    // CLEANUP
    // ==============================================

    @Override
    protected void onDestroy() {

        handler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }
}
