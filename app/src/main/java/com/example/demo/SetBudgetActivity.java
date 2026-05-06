package com.example.demo;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.demo.data.UserRepository;
import com.example.demo.databinding.ActivitySetBudgetBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SetBudgetActivity extends AppCompatActivity {

    private ActivitySetBudgetBinding binding;
    private UserRepository userRepository;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetBudgetBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        userId = user.getUid();
        userRepository = new UserRepository();

        // Load existing budget if any
        userRepository.getMonthlyBudget(userId).observe(this, budget -> {
            if (budget != null && budget > 0) {
                binding.etBudget.setText(String.valueOf(budget));
            }
        });

        binding.btnSaveBudget.setOnClickListener(v -> saveBudget());
    }

    private void saveBudget() {
        String budgetStr = binding.etBudget.getText().toString().trim();

        if (TextUtils.isEmpty(budgetStr)) {
            binding.tilBudget.setError("Budget cannot be empty");
            return;
        }

        try {
            double budget = Double.parseDouble(budgetStr);
            if (budget <= 0) {
                binding.tilBudget.setError("Budget must be positive");
                return;
            }

            // Save to Firestore layout
            userRepository.setMonthlyBudget(userId, budget);
            Toast.makeText(this, "Budget Saved Successfully!", Toast.LENGTH_SHORT).show();
            finish();

        } catch (NumberFormatException e) {
            binding.tilBudget.setError("Invalid amount");
        }
    }
}
