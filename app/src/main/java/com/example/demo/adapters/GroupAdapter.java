package com.example.demo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.R;
import com.example.demo.model.Group;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for the list of groups the user belongs to.
 */
public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    private List<Group> groups = new ArrayList<>();
    private final OnGroupClickListener listener;
    private String currentUserId;

    public GroupAdapter(OnGroupClickListener listener, String currentUserId) {
        this.listener = listener;
        this.currentUserId = currentUserId;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups != null ? groups : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groups.get(position);
        holder.bind(group, listener, currentUserId);
    }

    @Override
    public int getItemCount() { return groups.size(); }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvGroupName;
        private final TextView tvMemberCount;
        private final TextView tvBalance;
        private final View ivGroupIcon;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tv_group_name);
            tvMemberCount = itemView.findViewById(R.id.tv_member_count);
            tvBalance = itemView.findViewById(R.id.tv_group_balance);
            ivGroupIcon = itemView.findViewById(R.id.v_group_icon);
        }

        void bind(Group group, OnGroupClickListener listener, String currentUserId) {
            tvGroupName.setText(group.getName());
            int memberCount = group.getMembers() != null ? group.getMembers().size() : 0;
            tvMemberCount.setText(memberCount + " member" + (memberCount != 1 ? "s" : ""));

            // Balance will be calculated externally and passed through tag
            // For now show placeholder
            tvBalance.setText("Tap to view");
            tvBalance.setTextColor(itemView.getContext().getColor(R.color.colorOnSurfaceVariant));

            itemView.setOnClickListener(v -> listener.onGroupClick(group));
        }
    }
}
