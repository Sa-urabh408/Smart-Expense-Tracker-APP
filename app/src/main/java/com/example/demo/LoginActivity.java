package com.example.demo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.demo.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Login Activity - handles user authentication via Firebase Auth.
 * Redirects to MainActivity if user is already logged in.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMain();
            return;
        }

        setupClickListeners();
    }

    /**
     * Set up button click listeners for login and signup navigation.
     */
    private void setupClickListeners() {
        // Login button click
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        // Navigate to Signup
        binding.tvSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Forgot Password
        binding.tvForgotPassword.setOnClickListener(v -> forgotPassword());
    }

    /**
     * Send a password reset email via Firebase Auth.
     * User enters email in the email field, taps Forgot Password, gets reset link.
     */
    private void forgotPassword() {
        String email = binding.etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Enter your email first");
            binding.etEmail.requestFocus();
            return;
        }

        binding.tilEmail.setError(null);
        showLoading(true);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "✅ Password reset link sent to " + email + "\nCheck your email inbox!",
                                Toast.LENGTH_LONG).show();
                    } else {
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Failed to send reset email";
                        Toast.makeText(LoginActivity.this,
                                "❌ " + error,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Validate inputs and attempt Firebase login.
     */
    private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.error_password_short));
            return;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.error_password_short));
            return;
        }

        // Clear errors
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        // Show progress
        showLoading(true);

        // Firebase sign in
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        navigateToMain();
                    } else {
                        String errorMsg = task.getException() != null ? 
                                task.getException().getMessage() : getString(R.string.error_login_failed);
                        Toast.makeText(LoginActivity.this,
                                "Login Failed: " + errorMsg,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Navigate to MainActivity and finish LoginActivity.
     */
    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    /**
     * Show or hide loading indicator.
     */
    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!show);
    }
}
