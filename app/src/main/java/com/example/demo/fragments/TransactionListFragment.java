package com.example.demo.fragments;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.MainActivity;
import com.example.demo.R;
import com.example.demo.adapters.TransactionAdapter;
import com.example.demo.data.TransactionRepository;
import com.example.demo.databinding.FragmentTransactionListBinding;
import com.example.demo.model.Transaction;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Transaction List Fragment - displays all transactions with search,
 * filter, and swipe-to-delete functionality.
 */
public class TransactionListFragment extends Fragment implements TransactionAdapter.OnTransactionClickListener {

    private FragmentTransactionListBinding binding;
    private TransactionRepository repository;
    private TransactionAdapter adapter;
    private String userId;
    private String currentFilter = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionListBinding.inflate(inflater, container, false);
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

        // Setup RecyclerView
        setupRecyclerView();

        // Setup search
        setupSearch();

        // Setup filter chips
        setupFilterChips();

        // Load all transactions by default
        loadTransactions();
    }

    /**
     * Set up RecyclerView with adapter and swipe-to-delete.
     */
    private void setupRecyclerView() {
        adapter = new TransactionAdapter(this);
        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTransactions.setAdapter(adapter);

        // Swipe to delete
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Transaction removed = adapter.removeItem(position);
                if (removed != null) {
                    // Show undo snackbar
                    Snackbar.make(binding.getRoot(), R.string.transaction_deleted, Snackbar.LENGTH_LONG)
                            .setAction(R.string.undo, v -> {
                                adapter.restoreItem(removed, position);
                            })
                            .addCallback(new Snackbar.Callback() {
                                @Override
                                public void onDismissed(Snackbar snackbar, int event) {
                                    if (event != DISMISS_EVENT_ACTION) {
                                        // Actually delete from database
                                        repository.delete(removed);
                                    }
                                }
                            })
                            .setActionTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                            .show();
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                // Draw red background while swiping
                View itemView = viewHolder.itemView;
                Paint paint = new Paint();
                paint.setColor(ContextCompat.getColor(requireContext(), R.color.colorExpense));

                RectF background = new RectF(
                        itemView.getRight() + dX,
                        itemView.getTop(),
                        itemView.getRight(),
                        itemView.getBottom()
                );
                float cornerRadius = 12f * requireContext().getResources().getDisplayMetrics().density;
                c.drawRoundRect(background, cornerRadius, cornerRadius, paint);

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvTransactions);
    }

    /**
     * Set up search functionality with text change listener.
     */
    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    loadTransactions();
                } else {
                    searchTransactions(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Set up category filter chips.
     */
    private void setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentFilter = "All";
                loadTransactions();
                return;
            }

            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipAll) currentFilter = "All";
            else if (checkedId == R.id.chipFood) currentFilter = "Food";
            else if (checkedId == R.id.chipTravel) currentFilter = "Travel";
            else if (checkedId == R.id.chipShopping) currentFilter = "Shopping";
            else if (checkedId == R.id.chipBills) currentFilter = "Bills";
            else if (checkedId == R.id.chipEntertainment) currentFilter = "Entertainment";
            else if (checkedId == R.id.chipHealth) currentFilter = "Health";

            if ("All".equals(currentFilter)) {
                loadTransactions();
            } else {
                filterByCategory(currentFilter);
            }
        });
    }

    /**
     * Load all transactions for the current user.
     */
    private void loadTransactions() {
        repository.getAllTransactions(userId).observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null && !transactions.isEmpty()) {
                adapter.setTransactions(transactions);
                binding.rvTransactions.setVisibility(View.VISIBLE);
                binding.tvEmptyState.setVisibility(View.GONE);
            } else {
                adapter.setTransactions(null);
                binding.rvTransactions.setVisibility(View.GONE);
                binding.tvEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Search transactions by title.
     */
    private void searchTransactions(String query) {
        repository.searchTransactions(userId, query).observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null && !transactions.isEmpty()) {
                adapter.setTransactions(transactions);
                binding.rvTransactions.setVisibility(View.VISIBLE);
                binding.tvEmptyState.setVisibility(View.GONE);
            } else {
                adapter.setTransactions(null);
                binding.rvTransactions.setVisibility(View.GONE);
                binding.tvEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Filter transactions by category.
     */
    private void filterByCategory(String category) {
        repository.getTransactionsByCategory(userId, category).observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null && !transactions.isEmpty()) {
                adapter.setTransactions(transactions);
                binding.rvTransactions.setVisibility(View.VISIBLE);
                binding.tvEmptyState.setVisibility(View.GONE);
            } else {
                adapter.setTransactions(null);
                binding.rvTransactions.setVisibility(View.GONE);
                binding.tvEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToEdit(transaction);
        }
    }

    @Override
    public void onTransactionLongClick(Transaction transaction, int position) {
        // Show delete confirmation dialog
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_transaction)
                .setMessage(R.string.delete_confirmation)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    repository.delete(transaction);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
