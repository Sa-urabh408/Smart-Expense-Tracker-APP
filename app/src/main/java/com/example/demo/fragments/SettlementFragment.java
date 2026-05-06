package com.example.demo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.demo.R;
import com.example.demo.adapters.SettlementAdapter;
import com.example.demo.databinding.FragmentSettlementBinding;
import com.example.demo.model.GroupExpense;
import com.example.demo.model.User;
import com.example.demo.viewmodel.GroupViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SettlementFragment — shows who owes whom and allows marking debts as settled.
 */
public class SettlementFragment extends Fragment implements SettlementAdapter.OnSettleClickListener {

    private FragmentSettlementBinding binding;
    private GroupViewModel viewModel;
    private SettlementAdapter settlementAdapter;
    private String groupId;
    private String currentUserId;
    private List<GroupExpense> latestExpenses = new ArrayList<>();
    private Map<String, User> membersMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettlementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        viewModel = new ViewModelProvider(requireActivity()).get(GroupViewModel.class);

        if (getArguments() != null) {
            groupId = getArguments().getString("group_id");
        }
        viewModel.setCurrentGroupId(groupId);

        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        setupRecyclerView();
        populateMembersMap();
        observeExpenses();
    }

    private void setupRecyclerView() {
        settlementAdapter = new SettlementAdapter(this, currentUserId);
        binding.rvSettlements.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSettlements.setAdapter(settlementAdapter);
    }

    private void populateMembersMap() {
        List<User> members = viewModel.getCurrentMembers();
        if (members != null) {
            for (User u : members) membersMap.put(u.getUserId(), u);
            settlementAdapter.setMembersMap(membersMap);
        }
    }

    private void observeExpenses() {
        viewModel.getGroupExpenses(groupId).observe(getViewLifecycleOwner(), expenses -> {
            if (expenses == null) return;
            latestExpenses = expenses;
            refreshSettlements();
        });
    }

    private void refreshSettlements() {
        List<GroupViewModel.Settlement> settlements =
                viewModel.computeSettlements(latestExpenses, currentUserId);
        settlementAdapter.setSettlements(settlements);

        if (settlements.isEmpty()) {
            binding.tvAllSettled.setVisibility(View.VISIBLE);
            binding.rvSettlements.setVisibility(View.GONE);
        } else {
            binding.tvAllSettled.setVisibility(View.GONE);
            binding.rvSettlements.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSettleClick(GroupViewModel.Settlement settlement, int position) {
        // Mark ALL unsettled expenses involving this pair as settled
        // Simple approach: mark all expenses paid by settlement.toUserId
        // and where settlement.fromUserId is in splitAmong
        for (GroupExpense expense : latestExpenses) {
            if (expense.isSettled()) continue;
            boolean paidByTo = expense.getPaidBy().equals(settlement.toUserId);
            boolean fromInSplit = expense.getSplitAmong() != null
                    && expense.getSplitAmong().contains(settlement.fromUserId);
            if (paidByTo && fromInSplit) {
                viewModel.settleExpense(groupId, expense.getExpenseId());
            }
        }
        Toast.makeText(requireContext(), "Marked as settled!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
