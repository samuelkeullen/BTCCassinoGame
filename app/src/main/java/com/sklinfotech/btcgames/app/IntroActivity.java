package com.sklinfotech.btcgames.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;

import com.sklinfotech.btcgames.R;
import com.sklinfotech.btcgames.lib.CommonActivity;
import com.sklinfotech.btcgames.settings.CurrencySettings;

public class IntroActivity extends CommonActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_intro);
    }

    private void startActivity() {
        startActivity(new Intent(this,
            CurrencySettings.getInstance(this).getAccountKey() == null ? CreateAccountActivity.class : MainActivity.class));
    }

    private void fadeIn() {
        final View view = findViewById(R.id.container);

        Animation a = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        a.setDuration(500);
        a.setAnimationListener(new AnimationListener() {

            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.VISIBLE);
                mHandler.postDelayed(() -> fadeOut(), 1500);
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }

        });
        view.startAnimation(a);
    }

    private void fadeOut() {
        final View view = findViewById(R.id.container);

        Animation a = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        a.setDuration(500);
        a.setAnimationListener(new AnimationListener() {

            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.INVISIBLE);
                startActivity();
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }

        });
        view.startAnimation(a);
    }

    @Override
    public void onResume() {
        super.onResume();
        mHandler.postDelayed(this::fadeIn, 1000);
    }
}
