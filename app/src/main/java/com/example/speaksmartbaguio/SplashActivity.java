package com.example.speaksmartbaguio;

import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.ybq.android.spinkit.SpinKitView;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.ThreeBounce;

public class SplashActivity extends AppCompatActivity {

    private static final String GROUP_NAME = "STARWARS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LinearLayout letterContainer = findViewById(R.id.letterContainer);
        TextView textGroupSub = findViewById(R.id.textGroupSub);

        // Animate group name letters
        for (char c : GROUP_NAME.toCharArray()) {
            TextView letter = new TextView(this);
            letter.setText(String.valueOf(c));
            letter.setTextSize(36f);
            letter.setTypeface(null, android.graphics.Typeface.BOLD);
            letter.setAlpha(0f);

            Shader shader = new LinearGradient(
                    0, 0, 0, letter.getTextSize(),
                    getResources().getColor(R.color.starwars_start),
                    getResources().getColor(R.color.starwars_end),
                    Shader.TileMode.CLAMP
            );
            letter.getPaint().setShader(shader);

            letterContainer.addView(letter);
        }

        animateLetters(letterContainer, 0);
        textGroupSub.animate().alpha(1f).setDuration(1000).setStartDelay(1200).start();

        SpinKitView spinKitView = findViewById(R.id.spinnerDots);
        Sprite threeBounce = new ThreeBounce();
        threeBounce.setColor(getResources().getColor(R.color.primary_element_yellow));
        spinKitView.setIndeterminateDrawable(threeBounce);

        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, HomeActivity.class));
            finish();
        }, 10000);
    }

    private void animateLetters(LinearLayout container, int index) {
        if (index >= container.getChildCount()) return;

        TextView letter = (TextView) container.getChildAt(index);
        letter.animate().alpha(1f).setDuration(150).withEndAction(() ->
                animateLetters(container, index + 1)
        ).start();
    }
}
