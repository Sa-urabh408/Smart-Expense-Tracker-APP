package com.example.demo.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.demo.R;
import com.example.demo.data.TransactionDao;
import com.example.demo.data.TransactionRepository;
import com.example.demo.databinding.FragmentReportsBinding;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Reports Fragment - displays visual analytics with PieChart for category
 * distribution and BarChart for income vs expense comparison.
 */
public class ReportsFragment extends Fragment {

    private FragmentReportsBinding binding;
    private TransactionRepository repository;
    private String userId;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    // Category colors
    private final int[] categoryColors = new int[]{
            0xFFFF7043, // Food
            0xFF42A5F5, // Travel
            0xFFAB47BC, // Shopping
            0xFFFFCA28, // Bills
            0xFF26C6DA, // Entertainment
            0xFF66BB6A, // Health
            0xFF78909C  // Other
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : "local_user";

        // Initialize repository
        repository = new TransactionRepository(requireActivity().getApplication());

        // Setup charts
        setupPieChart();
        setupBarChart();

        // Observe data
        observeData();
    }

    /**
     * Configure the PieChart appearance.
     */
    private void setupPieChart() {
        PieChart pieChart = binding.pieChart;
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setTransparentCircleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleAlpha(40);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.animateY(1200, Easing.EaseInOutCubic);
        pieChart.setDrawEntryLabels(false);
        pieChart.setExtraOffsets(20, 10, 20, 10);

        // Legend
        Legend legend = pieChart.getLegend();
        legend.setTextColor(Color.WHITE);
        legend.setTextSize(12f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setWordWrapEnabled(true);
    }

    /**
     * Configure the BarChart appearance.
     */
    private void setupBarChart() {
        BarChart barChart = binding.barChart;
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setFitBars(true);
        barChart.animateY(1200, Easing.EaseInOutCubic);
        barChart.setPinchZoom(false);
        barChart.setScaleEnabled(false);

        // X Axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setTextSize(12f);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        // Y Axis
        barChart.getAxisLeft().setTextColor(Color.WHITE);
        barChart.getAxisLeft().setTextSize(11f);
        barChart.getAxisLeft().setDrawGridLines(true);
        barChart.getAxisLeft().setGridColor(0x33FFFFFF);
        barChart.getAxisRight().setEnabled(false);

        // Legend
        Legend legend = barChart.getLegend();
        legend.setTextColor(Color.WHITE);
        legend.setTextSize(12f);
    }

    /**
     * Observe LiveData for chart data and monthly summary.
     */
    private void observeData() {
        // Category expense sums for pie chart
        repository.getCategoryExpenseSums(userId).observe(getViewLifecycleOwner(), categorySums -> {
            if (categorySums != null && !categorySums.isEmpty()) {
                updatePieChart(categorySums);
                binding.pieChart.setVisibility(View.VISIBLE);
                binding.tvNoPieData.setVisibility(View.GONE);
            } else {
                binding.pieChart.setVisibility(View.GONE);
                binding.tvNoPieData.setVisibility(View.VISIBLE);
            }
        });

        // Monthly data
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long monthStart = cal.getTimeInMillis();

        cal.add(Calendar.MONTH, 1);
        long monthEnd = cal.getTimeInMillis();

        // Monthly income
        repository.getMonthlyIncome(userId, monthStart, monthEnd).observe(getViewLifecycleOwner(), income -> {
            double incomeVal = income != null ? income : 0;
            binding.tvMonthlyIncome.setText(currencyFormat.format(incomeVal));
            updateMonthlyBalance();
        });

        // Monthly expenses
        repository.getMonthlyExpense(userId, monthStart, monthEnd).observe(getViewLifecycleOwner(), expense -> {
            double expenseVal = expense != null ? expense : 0;
            binding.tvMonthlyExpenses.setText(currencyFormat.format(expenseVal));
            updateMonthlyBalance();
        });

        // Income vs Expense bar chart data
        repository.getTotalIncome(userId).observe(getViewLifecycleOwner(), income -> {
            repository.getTotalExpenses(userId).observe(getViewLifecycleOwner(), expense -> {
                double incomeVal = income != null ? income : 0;
                double expenseVal = expense != null ? expense : 0;
                if (incomeVal > 0 || expenseVal > 0) {
                    updateBarChart(incomeVal, expenseVal);
                    binding.barChart.setVisibility(View.VISIBLE);
                    binding.tvNoBarData.setVisibility(View.GONE);
                } else {
                    binding.barChart.setVisibility(View.GONE);
                    binding.tvNoBarData.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    /**
     * Update pie chart with category expense data.
     */
    private void updatePieChart(List<TransactionDao.CategorySum> categorySums) {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (TransactionDao.CategorySum sum : categorySums) {
            entries.add(new PieEntry((float) sum.total, sum.category));
            colors.add(getCategoryColor(sum.category));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new PercentFormatter(binding.pieChart));
        dataSet.setSliceSpace(3f);

        PieData data = new PieData(dataSet);
        binding.pieChart.setData(data);
        binding.pieChart.invalidate();
    }

    /**
     * Update bar chart with income vs expense data.
     */
    private void updateBarChart(double income, double expense) {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, (float) income));
        entries.add(new BarEntry(1, (float) expense));

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(
                ContextCompat.getColor(requireContext(), R.color.colorIncome),
                ContextCompat.getColor(requireContext(), R.color.colorExpense)
        );
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);

        binding.barChart.setData(data);
        binding.barChart.getXAxis().setValueFormatter(
                new IndexAxisValueFormatter(new String[]{"Income", "Expense"})
        );
        binding.barChart.invalidate();
    }

    /**
     * Get color for a specific expense category.
     */
    private int getCategoryColor(String category) {
        switch (category) {
            case "Food": return categoryColors[0];
            case "Travel": return categoryColors[1];
            case "Shopping": return categoryColors[2];
            case "Bills": return categoryColors[3];
            case "Entertainment": return categoryColors[4];
            case "Health": return categoryColors[5];
            default: return categoryColors[6];
        }
    }

    /**
     * Calculate and display the monthly balance.
     */
    private void updateMonthlyBalance() {
        try {
            String incomeStr = binding.tvMonthlyIncome.getText().toString();
            String expenseStr = binding.tvMonthlyExpenses.getText().toString();
            double income = currencyFormat.parse(incomeStr).doubleValue();
            double expense = currencyFormat.parse(expenseStr).doubleValue();
            binding.tvMonthlyBalance.setText(currencyFormat.format(income - expense));
        } catch (Exception e) {
            binding.tvMonthlyBalance.setText(currencyFormat.format(0));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
