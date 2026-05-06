package com.example.demo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.demo.MainActivity;
import com.example.demo.R;
import com.example.demo.adapters.TransactionAdapter;
import com.example.demo.data.TransactionRepository;
import com.example.demo.data.UserRepository;
import com.example.demo.databinding.FragmentDashboardBinding;
import com.example.demo.model.Transaction;
import com.example.demo.utils.SubscriptionStorage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Dashboard Fragment - shows financial overview with balance cards,
 * monthly progress, and recent transactions list.
 */
public class DashboardFragment extends Fragment implements TransactionAdapter.OnTransactionClickListener {

    private FragmentDashboardBinding binding;
    private TransactionRepository repository;
    private UserRepository userRepository;
    private TransactionAdapter adapter;
    private String userId;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private double currentBudget = 0.0;
    private double currentSpent = 0.0;
    private double currentEarned = 0.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            String displayName = user.getDisplayName();
            binding.tvUserName.setText(displayName != null ? displayName : "User");
        } else {
            userId = "local_user";
        }

        // Initialize repositories
        repository = new TransactionRepository(requireActivity().getApplication());
        userRepository = new UserRepository();

        // Setup RecyclerView
        setupRecyclerView();

        // Observe data
        observeData();

        // View All click
        binding.tvViewAll.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTransactions();
            }
        });

        // Subscription Strip
        refreshSubscriptionStrip();
        binding.subscriptionStrip.getRoot().setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToSubscriptions();
            }
        });
    }

    /**
     * Set up the recent transactions RecyclerView.
     */
    private void setupRecyclerView() {
        adapter = new TransactionAdapter(this);
        binding.rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentTransactions.setAdapter(adapter);
        binding.rvRecentTransactions.setNestedScrollingEnabled(false);
    }

    /**
     * Observe LiveData from the repository and update UI.
     */
    private void observeData() {
        // Total Income
        repository.getTotalIncome(userId).observe(getViewLifecycleOwner(), income -> {
            double incomeVal = income != null ? income : 0;
            binding.tvTotalIncome.setText(currencyFormat.format(incomeVal));
            updateBalance();
        });

        // Total Expenses
        repository.getTotalExpenses(userId).observe(getViewLifecycleOwner(), expenses -> {
            double expenseVal = expenses != null ? expenses : 0;
            binding.tvTotalExpenses.setText(currencyFormat.format(expenseVal));
            updateBalance();
        });

        // Recent Transactions
        repository.getRecentTransactions(userId).observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null && !transactions.isEmpty()) {
                adapter.setTransactions(transactions);
                binding.rvRecentTransactions.setVisibility(View.VISIBLE);
                binding.tvEmptyState.setVisibility(View.GONE);
            } else {
                binding.rvRecentTransactions.setVisibility(View.GONE);
                binding.tvEmptyState.setVisibility(View.VISIBLE);
            }
        });

        // Monthly data
        observeMonthlyData();
    }

    /**
     * Observe monthly income and expense for progress bar.
     */
    private void observeMonthlyData() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long monthStart = cal.getTimeInMillis();

        cal.add(Calendar.MONTH, 1);
        long monthEnd = cal.getTimeInMillis();

        repository.getMonthlyIncome(userId, monthStart, monthEnd).observe(getViewLifecycleOwner(), income -> {
            currentEarned = income != null ? income : 0;
            updateMonthlyProgress();
            calculateHealthScore();
        });

        repository.getMonthlyExpense(userId, monthStart, monthEnd).observe(getViewLifecycleOwner(), expense -> {
            currentSpent = expense != null ? expense : 0;
            binding.tvMonthlySpent.setText("Spent: " + currencyFormat.format(currentSpent));
            updateMonthlyProgress();
            calculateHealthScore();
        });

        userRepository.getMonthlyBudget(userId).observe(getViewLifecycleOwner(), budget -> {
            currentBudget = budget != null ? budget : 0;
            if (currentBudget > 0) {
                binding.tvMonthlyBudget.setText("Budget: " + currencyFormat.format(currentBudget));
            } else {
                binding.tvMonthlyBudget.setText("Budget: Not set");
            }
            updateMonthlyProgress();
            calculateHealthScore();
        });
    }

    /**
     * Calculate and update the total balance display.
     */
    private void updateBalance() {
        try {
            String incomeStr = binding.tvTotalIncome.getText().toString();
            String expenseStr = binding.tvTotalExpenses.getText().toString();
            double income = currencyFormat.parse(incomeStr).doubleValue();
            double expense = currencyFormat.parse(expenseStr).doubleValue();
            binding.tvTotalBalance.setText(currencyFormat.format(income - expense));
        } catch (Exception e) {
            binding.tvTotalBalance.setText(currencyFormat.format(0));
        }
    }

    /**
     * Update the monthly progress bar based on budget if set, or income vs expense
     * ratio.
     */
    private void updateMonthlyProgress() {
        try {
            if (currentBudget > 0) {
                int progress = (int) Math.min((currentSpent / currentBudget) * 100, 100);
                binding.progressMonthly.setProgress(progress);

                if (currentSpent >= currentBudget * 0.8) {
                    binding.tvBudgetWarning.setVisibility(View.VISIBLE);
                } else {
                    binding.tvBudgetWarning.setVisibility(View.GONE);
                }
            } else if (currentEarned > 0) {
                int progress = (int) Math.min((currentSpent / currentEarned) * 100, 100);
                binding.progressMonthly.setProgress(progress);
                binding.tvBudgetWarning.setVisibility(View.GONE);
            } else {
                binding.progressMonthly.setProgress(currentSpent > 0 ? 100 : 0);
                binding.tvBudgetWarning.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            binding.progressMonthly.setProgress(0);
        }
    }

    /**
     * Calculate the financial health score out of 100.
     */
    private void calculateHealthScore() {
        int score = 0;
        String suggestion = "";

        if (currentBudget > 0) {
            // Base score for having a budget
            score += 40;

            // Adjust score based on budget usage
            double usageRatio = currentSpent / currentBudget;
            if (usageRatio <= 0.5) {
                score += 60;
                suggestion = "Excellent! You are well under budget.";
            } else if (usageRatio <= 0.8) {
                score += 40;
                suggestion = "Good standing. Keep an eye on your spending.";
            } else if (usageRatio <= 1.0) {
                score += 20;
                suggestion = "Warning: Approaching your budget limit.";
            } else {
                score += 0;
                suggestion = "Critical: You've exceeded your monthly budget.";
            }
        } else {
            // Logic if no budget is set
            suggestion = "Set a monthly budget to improve your score.";
            if (currentEarned > 0) {
                double usageRatio = currentSpent / currentEarned;
                if (usageRatio <= 0.5) {
                    score = 70;
                } else if (usageRatio <= 0.8) {
                    score = 50;
                } else if (usageRatio <= 1.0) {
                    score = 30;
                } else {
                    score = 10;
                }
            } else {
                score = (currentSpent > 0) ? 0 : 50;
            }
        }

        binding.tvHealthScore.setText(String.valueOf(score));
        binding.tvHealthSuggestion.setText(suggestion);
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToEdit(transaction);
        }
    }

    @Override
    public void onTransactionLongClick(Transaction transaction, int position) {
        // No action on dashboard
    }

    /**
     * Refresh the subscription strip with latest data.
     */
    private void refreshSubscriptionStrip() {
        if (binding == null || getContext() == null) return;

        android.content.Context ctx = requireContext();
        java.util.List<com.example.demo.models.Subscription> subs = SubscriptionStorage.load(ctx);
        double monthly = SubscriptionStorage.totalMonthly(ctx);
        int active = SubscriptionStorage.activeCount(ctx);
        int dueSoon = SubscriptionStorage.dueSoon(ctx).size();

        binding.subscriptionStrip.tvStripMonthly.setText("₹" + formatIndian(monthly) + "/mo");
        binding.subscriptionStrip.tvStripActive.setText(active + " active plans");

        if (dueSoon > 0) {
            binding.subscriptionStrip.tvStripDue.setText("⚡" + dueSoon);
            binding.subscriptionStrip.tvStripDue.setVisibility(android.view.View.VISIBLE);
        } else {
            binding.subscriptionStrip.tvStripDue.setVisibility(android.view.View.GONE);
        }
    }

    private String formatIndian(double amount) {
        long value = (long) amount;
        if (value < 1000) return String.valueOf(value);
        String s = String.valueOf(value);
        int len = s.length();
        StringBuilder result = new StringBuilder();
        result.insert(0, s.substring(len - 3));
        int remaining = len - 3;
        while (remaining > 0) {
            int start = Math.max(0, remaining - 2);
            result.insert(0, s.substring(start, remaining) + ",");
            remaining = start;
        }
        return result.toString();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshSubscriptionStrip();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
