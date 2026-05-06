package com.example.demo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.example.demo.databinding.ActivitySplashBinding;

/**
 * Splash Screen Activity - shown on app startup with a smooth fade-in animation.
 */
public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Animation
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(1500);
        binding.ivLogo.startAnimation(fadeIn);
        binding.tvAppName.startAnimation(fadeIn);

        // Transition to MainActivity after 2.5 seconds
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 2500);
    }
}
