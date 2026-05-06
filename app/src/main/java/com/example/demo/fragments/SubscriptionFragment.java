package com.example.demo.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.demo.R;
import com.example.demo.adapters.SubscriptionAdapter;
import com.example.demo.databinding.FragmentSubscriptionBinding;
import com.example.demo.models.Subscription;
import com.example.demo.utils.SubscriptionStorage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Dedicated Subscription management screen.
 * Shows summary band, category filter chips, alert banner, and full subscription list.
 */
public class SubscriptionFragment extends Fragment
        implements SubscriptionAdapter.OnSubscriptionClickListener {

    private FragmentSubscriptionBinding binding;
    private SubscriptionAdapter adapter;
    private List<Subscription> allSubscriptions = new ArrayList<>();
    private String currentFilter = "ALL";

    // For the add/edit dialog date selection
    private long selectedRenewalDate = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSubscriptionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupClickListeners();
        setupChipFilters();
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new SubscriptionAdapter(requireContext(), this);
        binding.rvSubscriptions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSubscriptions.setAdapter(adapter);
        binding.rvSubscriptions.setNestedScrollingEnabled(false);
    }

    private void setupClickListeners() {
        // Back button
        binding.btnSubBack.setOnClickListener(v -> {
            if (getActivity() instanceof com.example.demo.MainActivity) {
                ((com.example.demo.MainActivity) getActivity()).onBackPressed();
            }
        });

        // FAB and top-right add button
        binding.fabAdd.setOnClickListener(v -> showAddEditDialog(null));
        binding.btnSubAddTop.setOnClickListener(v -> showAddEditDialog(null));
    }

    private void setupChipFilters() {
        View.OnClickListener chipClick = v -> {
            // Reset all chips
            resetChipStyles();

            // Activate selected
            ((TextView) v).setBackgroundResource(R.drawable.bg_chip_active);
            ((TextView) v).setTextColor(ContextCompat.getColor(requireContext(), R.color.subPurpleLight));

            // Set filter
            int id = v.getId();
            if (id == R.id.chipAll)            currentFilter = "ALL";
            else if (id == R.id.chipEntertainment) currentFilter = "ENTERTAINMENT";
            else if (id == R.id.chipWork)      currentFilter = "PRODUCTIVITY";
            else if (id == R.id.chipCloud)      currentFilter = "CLOUD";
            else if (id == R.id.chipHealth)     currentFilter = "HEALTH";
            else if (id == R.id.chipOther)      currentFilter = "OTHER";

            applyFilter();
        };

        binding.chipAll.setOnClickListener(chipClick);
        binding.chipEntertainment.setOnClickListener(chipClick);
        binding.chipWork.setOnClickListener(chipClick);
        binding.chipCloud.setOnClickListener(chipClick);
        binding.chipHealth.setOnClickListener(chipClick);
        binding.chipOther.setOnClickListener(chipClick);
    }

    private void resetChipStyles() {
        int[] chipIds = {R.id.chipAll, R.id.chipEntertainment, R.id.chipWork,
                R.id.chipCloud, R.id.chipHealth, R.id.chipOther};
        for (int chipId : chipIds) {
            TextView chip = binding.getRoot().findViewById(chipId);
            if (chip != null) {
                chip.setBackgroundResource(R.drawable.bg_chip_inactive);
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary));
            }
        }
    }

    // ── Data ─────────────────────────────────────────────────────────────────

    private void loadData() {
        allSubscriptions = SubscriptionStorage.load(requireContext());
        updateSummary();
        updateAlertBanner();
        applyFilter();
    }

    private void updateSummary() {
        double totalMonthly = 0;
        int activeCount = 0;
        int dueSoonCount = 0;

        for (Subscription s : allSubscriptions) {
            if (s.isActive()) {
                activeCount++;
                totalMonthly += s.monthlyPrice();
                if (s.daysLeft() <= 7) dueSoonCount++;
            }
        }

        binding.tvSubTotalMonthly.setText("₹" + formatIndian(totalMonthly));
        binding.tvSubActiveCount.setText(String.valueOf(activeCount));
        binding.tvSubDueSoon.setText(String.valueOf(dueSoonCount));
    }

    private void updateAlertBanner() {
        Subscription mostUrgent = null;
        for (Subscription s : allSubscriptions) {
            if (s.isActive() && s.daysLeft() <= 3) {
                if (mostUrgent == null || s.daysLeft() < mostUrgent.daysLeft()) {
                    mostUrgent = s;
                }
            }
        }

        if (mostUrgent != null) {
            binding.alertBanner.setVisibility(View.VISIBLE);
            String msg;
            if (mostUrgent.daysLeft() <= 1) {
                msg = "⚡ " + mostUrgent.getName() + " kal renew — ₹"
                        + formatIndian(mostUrgent.getPrice()) + " ready rakho!";
            } else {
                msg = "⚡ " + mostUrgent.getName() + " " + mostUrgent.daysLeft()
                        + " din mein renew — ₹" + formatIndian(mostUrgent.getPrice());
            }
            binding.tvAlertMessage.setText(msg);
        } else {
            binding.alertBanner.setVisibility(View.GONE);
        }
    }

    private void applyFilter() {
        List<Subscription> filtered;
        if ("ALL".equals(currentFilter)) {
            filtered = new ArrayList<>(allSubscriptions);
        } else {
            filtered = new ArrayList<>();
            for (Subscription s : allSubscriptions) {
                if (s.getCategory() != null
                        && s.getCategory().name().equals(currentFilter)) {
                    filtered.add(s);
                }
            }
        }

        adapter.setSubscriptions(filtered);

        // Show/hide empty state
        if (filtered.isEmpty()) {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.rvSubscriptions.setVisibility(View.GONE);
        } else {
            binding.emptyState.setVisibility(View.GONE);
            binding.rvSubscriptions.setVisibility(View.VISIBLE);
        }
    }

    // ── Add/Edit Dialog ──────────────────────────────────────────────────────

    private void showAddEditDialog(@Nullable Subscription existing) {
        boolean isEdit = existing != null;

        // Build dialog layout programmatically
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);
        layout.setBackgroundColor(0xFF1C1C1E);

        // Name field
        EditText etName = createEditText("Service name (e.g. Netflix)");
        layout.addView(etName);

        // Emoji field
        EditText etEmoji = createEditText("Emoji (e.g. 🎬)");
        layout.addView(etEmoji);

        // Price field
        EditText etPrice = createEditText("Price (e.g. 649)");
        etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etPrice);

        // Billing cycle spinner
        Spinner spCycle = new Spinner(requireContext());
        String[] cycles = {"Monthly", "Weekly", "Yearly"};
        ArrayAdapter<String> cycleAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, cycles);
        spCycle.setAdapter(cycleAdapter);
        layout.addView(spCycle);

        // Category spinner
        Spinner spCategory = new Spinner(requireContext());
        String[] categories = {"Entertainment", "Productivity", "Cloud", "Health", "Other"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, categories);
        spCategory.setAdapter(catAdapter);
        LinearLayout.LayoutParams catParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        catParams.topMargin = 16;
        spCategory.setLayoutParams(catParams);
        layout.addView(spCategory);

        // Renewal date button
        TextView tvDate = new TextView(requireContext());
        tvDate.setPadding(0, 32, 0, 16);
        tvDate.setTextColor(0xFFA78BFA);
        tvDate.setTextSize(15);
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

        // Default date: 30 days from now
        Calendar defaultCal = Calendar.getInstance();
        defaultCal.add(Calendar.DAY_OF_YEAR, 30);
        selectedRenewalDate = defaultCal.getTimeInMillis();
        tvDate.setText("📅 Renewal: " + sdf.format(new Date(selectedRenewalDate)));

        tvDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedRenewalDate);
            new DatePickerDialog(requireContext(), (dp, y, m, d) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(y, m, d, 0, 0, 0);
                selectedRenewalDate = selected.getTimeInMillis();
                tvDate.setText("📅 Renewal: " + sdf.format(new Date(selectedRenewalDate)));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });
        layout.addView(tvDate);

        // Notes field
        EditText etNotes = createEditText("Notes (optional)");
        layout.addView(etNotes);

        // Pre-fill for edit
        if (isEdit) {
            etName.setText(existing.getName());
            etEmoji.setText(existing.getEmoji());
            etPrice.setText(String.valueOf(existing.getPrice()));
            selectedRenewalDate = existing.getRenewalDateMillis();
            tvDate.setText("📅 Renewal: " + sdf.format(new Date(selectedRenewalDate)));
            etNotes.setText(existing.getNotes());

            // Set spinners
            if (existing.getCycle() != null) {
                int cycleIdx = existing.getCycle().ordinal();
                // Map: WEEKLY=0, MONTHLY=1, YEARLY=2 vs spinner: Monthly=0, Weekly=1, Yearly=2
                switch (existing.getCycle()) {
                    case MONTHLY: spCycle.setSelection(0); break;
                    case WEEKLY:  spCycle.setSelection(1); break;
                    case YEARLY:  spCycle.setSelection(2); break;
                }
            }
            if (existing.getCategory() != null) {
                switch (existing.getCategory()) {
                    case ENTERTAINMENT: spCategory.setSelection(0); break;
                    case PRODUCTIVITY:  spCategory.setSelection(1); break;
                    case CLOUD:         spCategory.setSelection(2); break;
                    case HEALTH:        spCategory.setSelection(3); break;
                    case OTHER:         spCategory.setSelection(4); break;
                }
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(),
                com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog);
        builder.setTitle(isEdit ? "Edit Subscription" : "Add Subscription");
        builder.setView(layout);

        builder.setPositiveButton(isEdit ? "Update" : "Add", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String emoji = etEmoji.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(requireContext(), R.string.error_empty_fields,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            double price;
            try {
                price = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid price",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Parse cycle
            Subscription.BillingCycle cycle;
            switch (spCycle.getSelectedItemPosition()) {
                case 1:  cycle = Subscription.BillingCycle.WEEKLY; break;
                case 2:  cycle = Subscription.BillingCycle.YEARLY; break;
                default: cycle = Subscription.BillingCycle.MONTHLY; break;
            }

            // Parse category
            Subscription.Category category;
            switch (spCategory.getSelectedItemPosition()) {
                case 0:  category = Subscription.Category.ENTERTAINMENT; break;
                case 1:  category = Subscription.Category.PRODUCTIVITY; break;
                case 2:  category = Subscription.Category.CLOUD; break;
                case 3:  category = Subscription.Category.HEALTH; break;
                default: category = Subscription.Category.OTHER; break;
            }

            if (isEdit) {
                existing.setName(name);
                existing.setEmoji(emoji.isEmpty() ? "📦" : emoji);
                existing.setPrice(price);
                existing.setCycle(cycle);
                existing.setRenewalDateMillis(selectedRenewalDate);
                existing.setCategory(category);
                existing.setNotes(notes);
                SubscriptionStorage.update(requireContext(), existing);
                Toast.makeText(requireContext(), R.string.subscription_updated,
                        Toast.LENGTH_SHORT).show();
            } else {
                Subscription sub = new Subscription(name,
                        emoji.isEmpty() ? "📦" : emoji, price,
                        cycle, selectedRenewalDate, category);
                sub.setNotes(notes);
                SubscriptionStorage.add(requireContext(), sub);
                Toast.makeText(requireContext(), R.string.subscription_added,
                        Toast.LENGTH_SHORT).show();
            }
            loadData();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private EditText createEditText(String hint) {
        EditText et = new EditText(requireContext());
        et.setHint(hint);
        et.setHintTextColor(0xFF757575);
        et.setTextColor(0xFFFFFFFF);
        et.setTextSize(15);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = 8;
        et.setLayoutParams(params);
        return et;
    }

    // ── Adapter callbacks ────────────────────────────────────────────────────

    @Override
    public void onSubscriptionClick(Subscription sub) {
        showAddEditDialog(sub);
    }

    @Override
    public void onSubscriptionLongClick(Subscription sub, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_subscription)
                .setMessage(R.string.delete_sub_confirm)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    SubscriptionStorage.delete(requireContext(), sub.getId());
                    Toast.makeText(requireContext(), R.string.subscription_deleted,
                            Toast.LENGTH_SHORT).show();
                    loadData();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
