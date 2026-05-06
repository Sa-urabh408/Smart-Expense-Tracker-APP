package com.example.demo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.demo.R;
import com.example.demo.adapters.GroupExpenseAdapter;
import com.example.demo.databinding.FragmentGroupDetailBinding;
import com.example.demo.model.User;
import com.example.demo.viewmodel.GroupViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * GroupDetailFragment — shows all members, expenses and the Settle Up button.
 */
public class GroupDetailFragment extends Fragment {

    private FragmentGroupDetailBinding binding;
    private GroupViewModel viewModel;
    private GroupExpenseAdapter expenseAdapter;
    private String groupId;
    private String groupName;
    private String currentUserId;
    private Map<String, User> membersMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGroupDetailBinding.inflate(inflater, container, false);
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
            groupName = getArguments().getString("group_name");
        }

        if (viewModel.getCurrentGroup() == null || !groupId.equals(viewModel.getCurrentGroupId())) {
            viewModel.fetchGroupDetails(groupId);
        }

        binding.toolbar.setTitle(groupName != null ? groupName : "Group");
        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        binding.toolbar.inflateMenu(R.menu.menu_group_detail);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_leave_group) {
                confirmLeaveGroup();
                return true;
            }
            return false;
        });

        setupExpensesRecyclerView();
        loadMembers();
        observeExpenses();

        binding.fabAddExpense.setOnClickListener(v -> openAddExpense());
        binding.btnSettleUp.setOnClickListener(v -> openSettlement());
    }

    private void setupExpensesRecyclerView() {
        expenseAdapter = new GroupExpenseAdapter();
        binding.rvGroupExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvGroupExpenses.setAdapter(expenseAdapter);
    }

    private void loadMembers() {
        List<String> memberIds;
        if (viewModel.getCurrentGroup() != null) {
            memberIds = viewModel.getCurrentGroup().getMembers();
        } else {
            // Fallback if detail fetch is still pending — the observer on details in ViewModel will eventually update currentMembers
            return; 
        }
        viewModel.getGroupMembers(memberIds).observe(getViewLifecycleOwner(), users -> {
            if (users == null) return;
            viewModel.setCurrentMembers(users);
            membersMap.clear();
            for (User u : users) membersMap.put(u.getUserId(), u);
            expenseAdapter.setMembersMap(membersMap);
            renderMemberAvatars(users);
        });
    }

    private void renderMemberAvatars(List<User> users) {
        binding.memberAvatarContainer.removeAllViews();
        for (User u : users) {
            View avatar = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_member_avatar, binding.memberAvatarContainer, false);
            android.widget.TextView tv = avatar.findViewById(R.id.tv_avatar_initial);
            tv.setText(u.getInitial());
            android.widget.TextView name = avatar.findViewById(R.id.tv_avatar_name);
            name.setText(u.getBestDisplayName());
            binding.memberAvatarContainer.addView(avatar);
        }
    }

    private void observeExpenses() {
        viewModel.getGroupExpenses(groupId).observe(getViewLifecycleOwner(), expenses -> {
            boolean hasExpenses = expenses != null && !expenses.isEmpty();
            if (hasExpenses) {
                expenseAdapter.setExpenses(expenses);
                binding.rvGroupExpenses.setVisibility(View.VISIBLE);
                binding.tvNoExpenses.setVisibility(View.GONE);

                // Compute and display my balance
                double myBalance = viewModel.computeMyBalance(expenses, currentUserId);
                updateBalanceSummary(myBalance);
            } else {
                binding.rvGroupExpenses.setVisibility(View.GONE);
                binding.tvNoExpenses.setVisibility(View.VISIBLE);
                binding.tvNoExpenses.setText("No expenses yet. Tap + to add one.");
                binding.tvBalanceSummary.setText("You are all settled up \uD83C\uDF89");
                binding.tvBalanceSummary.setTextColor(requireContext().getColor(R.color.colorPrimary));
            }
        });
    }

    private void updateBalanceSummary(double balance) {
        if (balance > 0.01) {
            binding.tvBalanceSummary.setText(
                    String.format(Locale.getDefault(), "You are owed ₹%.2f", balance));
            binding.tvBalanceSummary.setTextColor(
                    requireContext().getColor(R.color.colorIncome));
        } else if (balance < -0.01) {
            binding.tvBalanceSummary.setText(
                    String.format(Locale.getDefault(), "You owe ₹%.2f", -balance));
            binding.tvBalanceSummary.setTextColor(
                    requireContext().getColor(R.color.colorExpense));
        } else {
            binding.tvBalanceSummary.setText("You are all settled up \uD83C\uDF89");
            binding.tvBalanceSummary.setTextColor(
                    requireContext().getColor(R.color.colorPrimary));
        }
    }

    private void openAddExpense() {
        Bundle args = new Bundle();
        args.putString("group_id", groupId);

        AddGroupExpenseFragment fragment = new AddGroupExpenseFragment();
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack("add_expense")
                .commit();
    }

    private void openSettlement() {
        SettlementFragment settlementFragment = new SettlementFragment();
        Bundle args = new Bundle();
        args.putString("group_id", groupId);
        settlementFragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(R.id.fragmentContainer, settlementFragment)
                .addToBackStack("settlement")
                .commit();
    }

    private void confirmLeaveGroup() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Leave Group")
                .setMessage("Are you sure you want to leave \"" + groupName + "\"?")
                .setPositiveButton("Leave", (d, w) ->
                        viewModel.leaveGroup(groupId, currentUserId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
