package com.example.demo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.R;
import com.example.demo.adapters.GroupAdapter;
import com.example.demo.databinding.FragmentGroupBinding;
import com.example.demo.model.Group;
import com.example.demo.viewmodel.GroupViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * GroupFragment — shows the user's groups list and a FAB to create a new group.
 */
public class GroupFragment extends Fragment implements GroupAdapter.OnGroupClickListener {

    private FragmentGroupBinding binding;
    private GroupViewModel viewModel;
    private GroupAdapter adapter;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGroupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        userId = user.getUid();

        viewModel = new ViewModelProvider(requireActivity()).get(GroupViewModel.class);

        setupRecyclerView();
        observeGroups();

        binding.fabCreateGroup.setOnClickListener(v -> openCreateGroup());
    }

    private void setupRecyclerView() {
        adapter = new GroupAdapter(this, userId);
        binding.rvGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvGroups.setAdapter(adapter);
    }

    private void observeGroups() {
        viewModel.getUserGroups(userId).observe(getViewLifecycleOwner(), groups -> {
            if (groups != null && !groups.isEmpty()) {
                adapter.setGroups(groups);
                binding.rvGroups.setVisibility(View.VISIBLE);
                binding.tvEmptyGroups.setVisibility(View.GONE);
            } else {
                binding.rvGroups.setVisibility(View.GONE);
                binding.tvEmptyGroups.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getOperationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Snackbar.make(binding.getRoot(), "Error: " + error, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void openCreateGroup() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(R.id.fragmentContainer, new CreateGroupFragment())
                .addToBackStack("create_group")
                .commit();
    }

    @Override
    public void onGroupClick(Group group) {
        viewModel.setCurrentGroupId(group.getGroupId());
        viewModel.setCurrentGroup(group);

        Bundle args = new Bundle();
        args.putString("group_id", group.getGroupId());
        args.putString("group_name", group.getName());

        GroupDetailFragment detailFragment = new GroupDetailFragment();
        detailFragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(R.id.fragmentContainer, detailFragment)
                .addToBackStack("group_detail")
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
