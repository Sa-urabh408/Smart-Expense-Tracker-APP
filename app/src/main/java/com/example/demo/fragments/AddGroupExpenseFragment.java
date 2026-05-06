package com.example.demo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.demo.R;
import com.example.demo.databinding.FragmentAddGroupExpenseBinding;
import com.example.demo.model.GroupExpense;
import com.example.demo.model.User;
import com.example.demo.viewmodel.GroupViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * AddGroupExpenseFragment — form to add a shared expense to a group.
 * Supports equal split (all members) or custom member selection.
 */
public class AddGroupExpenseFragment extends Fragment {

    private FragmentAddGroupExpenseBinding binding;
    private GroupViewModel viewModel;
    private String groupId;
    private String currentUserId;
    private List<User> members = new ArrayList<>();
    private List<CheckBox> memberCheckBoxes = new ArrayList<>();
    private boolean equalSplit = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddGroupExpenseBinding.inflate(inflater, container, false);
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
        if (viewModel.getCurrentMembers().isEmpty() && groupId != null) {
            viewModel.fetchGroupDetails(groupId);
        }
        members = viewModel.getCurrentMembers();

        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        setupPaidBySpinner();
        setupSplitToggle();
        buildMemberCheckboxes();

        binding.btnSaveExpense.setOnClickListener(v -> saveExpense());

        observeViewModel();
    }

    private void setupPaidBySpinner() {
        List<String> memberNames = new ArrayList<>();
        for (User u : members) memberNames.add(u.getBestDisplayName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, memberNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPaidBy.setAdapter(adapter);

        // Default to current user position
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getUserId().equals(currentUserId)) {
                binding.spinnerPaidBy.setSelection(i);
                break;
            }
        }
    }

    private void setupSplitToggle() {
        binding.radioEqualSplit.setChecked(true);
        equalSplit = true;
        binding.memberCheckboxContainer.setVisibility(View.GONE);

        binding.rgSplitType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_equal_split) {
                equalSplit = true;
                binding.memberCheckboxContainer.setVisibility(View.GONE);
            } else {
                equalSplit = false;
                binding.memberCheckboxContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    private void buildMemberCheckboxes() {
        binding.memberCheckboxContainer.removeAllViews();
        memberCheckBoxes.clear();

        for (User member : members) {
            CheckBox cb = new CheckBox(requireContext());
            cb.setText(member.getBestDisplayName());
            cb.setTextColor(requireContext().getColor(R.color.textPrimary));
            cb.setChecked(true);
            cb.setPadding(8, 8, 8, 8);
            memberCheckBoxes.add(cb);
            binding.memberCheckboxContainer.addView(cb);
        }
    }

    private void saveExpense() {
        String amountStr = binding.etAmount.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();

        if (amountStr.isEmpty()) {
            binding.etAmount.setError("Enter an amount");
            return;
        }
        if (description.isEmpty()) {
            binding.etDescription.setError("Enter a description");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            binding.etAmount.setError("Enter a valid positive amount");
            return;
        }

        int payerIndex = binding.spinnerPaidBy.getSelectedItemPosition();
        if (payerIndex < 0 || payerIndex >= members.size()) {
            Toast.makeText(requireContext(), "Select who paid", Toast.LENGTH_SHORT).show();
            return;
        }
        String paidBy = members.get(payerIndex).getUserId();

        List<String> splitAmong = new ArrayList<>();
        if (equalSplit) {
            for (User u : members) splitAmong.add(u.getUserId());
        } else {
            for (int i = 0; i < memberCheckBoxes.size(); i++) {
                if (memberCheckBoxes.get(i).isChecked()) {
                    splitAmong.add(members.get(i).getUserId());
                }
            }
            if (splitAmong.isEmpty()) {
                Toast.makeText(requireContext(), "Select at least one member to split with", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        GroupExpense expense = new GroupExpense(groupId, amount, description, paidBy, splitAmong);
        binding.btnSaveExpense.setEnabled(false);
        viewModel.resetOperationState();
        viewModel.addExpense(expense);
    }

    private void observeViewModel() {
        viewModel.getOperationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(requireContext(), "Expense added!", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        viewModel.getOperationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                binding.btnSaveExpense.setEnabled(true);
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
