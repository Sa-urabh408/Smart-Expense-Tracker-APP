package com.example.demo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.demo.databinding.ActivitySignupBinding;
import com.example.demo.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Signup Activity - handles new user registration via Firebase Auth.
 * After successful registration, writes user profile (email + displayName)
 * to the Firestore "users" collection so group member lookup by email works.
 */
public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnSignup.setOnClickListener(v -> attemptSignup());

        binding.tvLogin.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void attemptSignup() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.error_password_short));
            return;
        }
        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            return;
        }

        binding.tilName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        // Step 1: update Firebase Auth display name
                        UserProfileChangeRequest profileUpdates =
                                new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();

                        mAuth.getCurrentUser().updateProfile(profileUpdates)
                                .addOnCompleteListener(profileTask -> {
                                    // Step 2: write profile to Firestore users collection
                                    // This is REQUIRED for group member lookup by email
                                    String uid = mAuth.getCurrentUser().getUid();
                                    User userDoc = new User(uid, 0.0);
                                    userDoc.setEmail(email.toLowerCase().trim());
                                    userDoc.setDisplayName(name);

                                    FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(uid)
                                            .set(userDoc)
                                            .addOnCompleteListener(firestoreTask -> {
                                                showLoading(false);
                                                navigateToMain();
                                            });
                                });
                    } else {
                        showLoading(false);
                        String errorMsg = task.getException() != null ?
                                task.getException().getMessage() : getString(R.string.error_signup_failed);
                        Toast.makeText(SignupActivity.this,
                                "Signup Failed: " + errorMsg,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToMain() {
        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnSignup.setEnabled(!show);
    }
}
