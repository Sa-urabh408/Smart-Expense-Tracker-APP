package com.example.demo;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.demo.databinding.ActivityMainBinding;
import com.example.demo.fragments.AddTransactionFragment;
import com.example.demo.fragments.DashboardFragment;
import com.example.demo.fragments.GroupFragment;
import com.example.demo.fragments.ProfileFragment;
import com.example.demo.fragments.ReportsFragment;
import com.example.demo.fragments.SubscriptionFragment;
import com.example.demo.fragments.TransactionListFragment;
import com.example.demo.utils.NotificationHelper;
import com.example.demo.utils.SubscriptionNotificationManager;
import com.example.demo.workers.SubscriptionCheckWorker;
import com.example.demo.workers.WeeklyReportWorker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * MainActivity - hosts the BottomNavigationView and manages fragment switching.
 * Acts as the main navigation hub for the app after authentication.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;

    // Fragment instances
    private DashboardFragment dashboardFragment;
    private AddTransactionFragment addTransactionFragment;
    private TransactionListFragment transactionListFragment;
    private ReportsFragment reportsFragment;
    private ProfileFragment profileFragment;
    private GroupFragment groupFragment;
    private SubscriptionFragment subscriptionFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        initializeFragments();
        setupBottomNavigation();

        com.example.demo.data.TransactionRepository repository =
                new com.example.demo.data.TransactionRepository(getApplication());
        repository.syncFromFirestore(currentUser.getUid());

        // Ensure profile displayName + email are in Firestore (repairs corrupted docs)
        new com.example.demo.data.UserRepository().ensureProfileData(
                currentUser.getUid(),
                currentUser.getDisplayName(),
                currentUser.getEmail()
        );

        NotificationHelper.scheduleDailyReminder(this);
        scheduleWeeklyReport();

        // Subscription notifications
        SubscriptionNotificationManager.createNotificationChannel(this);
        scheduleSubscriptionCheck();
    }

    private void scheduleWeeklyReport() {
        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(WeeklyReportWorker.class, 7, TimeUnit.DAYS)
                        .setInitialDelay(7, TimeUnit.DAYS)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "WeeklyExpenseReport",
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    private void initializeFragments() {
        dashboardFragment = new DashboardFragment();
        addTransactionFragment = new AddTransactionFragment();
        transactionListFragment = new TransactionListFragment();
        reportsFragment = new ReportsFragment();
        profileFragment = new ProfileFragment();
        groupFragment = new GroupFragment();
        subscriptionFragment = new SubscriptionFragment();
        activeFragment = dashboardFragment;

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, profileFragment, "profile").hide(profileFragment)
                .add(R.id.fragmentContainer, reportsFragment, "reports").hide(reportsFragment)
                .add(R.id.fragmentContainer, groupFragment, "groups").hide(groupFragment)
                .add(R.id.fragmentContainer, subscriptionFragment, "subscriptions").hide(subscriptionFragment)
                .add(R.id.fragmentContainer, transactionListFragment, "transactions").hide(transactionListFragment)
                .add(R.id.fragmentContainer, addTransactionFragment, "add").hide(addTransactionFragment)
                .add(R.id.fragmentContainer, dashboardFragment, "dashboard")
                .commit();
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                selectedFragment = dashboardFragment;
            } else if (itemId == R.id.nav_add) {
                selectedFragment = addTransactionFragment;
            } else if (itemId == R.id.nav_groups) {
                selectedFragment = groupFragment;
            } else if (itemId == R.id.nav_reports) {
                selectedFragment = reportsFragment;
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = profileFragment;
            } else {
                return false;
            }

            // Clear group sub-screen back stack when leaving Groups tab
            if (itemId != R.id.nav_groups) {
                getSupportFragmentManager().popBackStack(null,
                        androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }

            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .hide(activeFragment)
                    .show(selectedFragment)
                    .commit();

            activeFragment = selectedFragment;
            return true;
        });
    }

    public void navigateToTransactions() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .hide(activeFragment)
                .show(transactionListFragment)
                .commit();
        activeFragment = transactionListFragment;
    }

    public void navigateToEdit(com.example.demo.model.Transaction transaction) {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_add);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .hide(activeFragment)
                .show(addTransactionFragment)
                .commit();
        activeFragment = addTransactionFragment;
        addTransactionFragment.editTransaction(transaction);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Navigate to the dedicated Subscription screen.
     */
    public void navigateToSubscriptions() {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .hide(activeFragment)
                .show(subscriptionFragment)
                .commit();
        activeFragment = subscriptionFragment;
    }

    /**
     * Schedule daily subscription renewal checks via WorkManager.
     */
    private void scheduleSubscriptionCheck() {
        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(SubscriptionCheckWorker.class, 24, TimeUnit.HOURS)
                        .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "sub_daily_check",
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    @Override
    public void onBackPressed() {
        // If on subscription screen, go back to dashboard
        if (activeFragment == subscriptionFragment) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .hide(activeFragment)
                    .show(dashboardFragment)
                    .commit();
            activeFragment = dashboardFragment;
            binding.bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
            return;
        }
        super.onBackPressed();
    }

    public void logout() {
        mAuth.signOut();
        navigateToLogin();
    }
}
