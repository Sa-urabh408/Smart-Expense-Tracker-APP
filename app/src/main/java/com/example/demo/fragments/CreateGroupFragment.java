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
import com.example.demo.adapters.MemberChipAdapter;
import com.example.demo.databinding.FragmentCreateGroupBinding;
import com.example.demo.model.Group;
import com.example.demo.model.User;
import com.example.demo.viewmodel.GroupViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * CreateGroupFragment — lets the user name a group and add members by email.
 */
public class CreateGroupFragment extends Fragment {

    private FragmentCreateGroupBinding binding;
    private GroupViewModel viewModel;
    private MemberChipAdapter chipAdapter;
    private String currentUserId;
    private String currentUserEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateGroupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        currentUserId = user.getUid();
        currentUserEmail = user.getEmail();

        viewModel = new ViewModelProvider(requireActivity()).get(GroupViewModel.class);

        setupMemberChips();
        setupListeners();
        observeViewModel();
    }

    private void setupMemberChips() {
        chipAdapter = new MemberChipAdapter((removedUser, position) -> {
            chipAdapter.removeMember(position);
        });
        binding.rvMembers.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.VERTICAL, false));
        binding.rvMembers.setAdapter(chipAdapter);
    }

    private void setupListeners() {
        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        binding.btnAddMember.setOnClickListener(v -> {
            String email = binding.etMemberEmail.getText().toString().trim().toLowerCase();
            if (email.isEmpty()) {
                binding.etMemberEmail.setError("Enter an email address");
                return;
            }
            if (email.equals(currentUserEmail)) {
                Toast.makeText(requireContext(), "You are already the group creator", Toast.LENGTH_SHORT).show();
                return;
            }
            searchAndAddMember(email);
        });

        binding.btnSaveGroup.setOnClickListener(v -> saveGroup());
    }

    private void searchAndAddMember(String email) {
        binding.btnAddMember.setEnabled(false);
        viewModel.findUserByEmail(email, new com.example.demo.data.GroupRepository.OnCompleteListener<User>() {
            @Override
            public void onSuccess(User user) {
                binding.btnAddMember.setEnabled(true);
                if (user == null) {
                    Toast.makeText(requireContext(),
                            "No user found with that email. They must register first.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    chipAdapter.addMember(user);
                    binding.etMemberEmail.setText("");
                    Toast.makeText(requireContext(),
                            user.getBestDisplayName() + " added", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                binding.btnAddMember.setEnabled(true);
                Toast.makeText(requireContext(), "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveGroup() {
        String name = binding.etGroupName.getText().toString().trim();
        if (name.isEmpty()) {
            binding.etGroupName.setError("Enter a group name");
            return;
        }

        List<User> members = chipAdapter.getMembers();
        List<String> memberIds = new ArrayList<>();
        memberIds.add(currentUserId); // creator is always a member

        for (User m : members) {
            if (!memberIds.contains(m.getUserId())) {
                memberIds.add(m.getUserId());
            }
        }

        Group group = new Group(name, currentUserId, memberIds);
        binding.btnSaveGroup.setEnabled(false);
        viewModel.resetOperationState();
        viewModel.createGroup(group);
    }

    private void observeViewModel() {
        viewModel.getOperationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(requireContext(), "Group created!", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        viewModel.getOperationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                binding.btnSaveGroup.setEnabled(true);
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
