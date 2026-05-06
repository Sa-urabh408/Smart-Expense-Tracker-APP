package com.example.demo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.R;
import com.example.demo.model.User;
import com.example.demo.utils.ProfileImageHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter showing selected members as chips in the CreateGroup screen.
 * Each chip has an ✕ button to remove the member.
 */
public class MemberChipAdapter extends RecyclerView.Adapter<MemberChipAdapter.ChipViewHolder> {

    public interface OnRemoveClickListener {
        void onRemoveClick(User user, int position);
    }

    private final List<User> members = new ArrayList<>();
    private final OnRemoveClickListener listener;

    public MemberChipAdapter(OnRemoveClickListener listener) {
        this.listener = listener;
    }

    public void addMember(User user) {
        // Avoid duplicates
        for (User m : members) {
            if (m.getUserId().equals(user.getUserId())) return;
        }
        members.add(user);
        notifyItemInserted(members.size() - 1);
    }

    public void removeMember(int position) {
        if (position >= 0 && position < members.size()) {
            members.remove(position);
            notifyItemRemoved(position);
        }
    }

    public List<User> getMembers() { return new ArrayList<>(members); }

    @NonNull
    @Override
    public ChipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member_chip, parent, false);
        return new ChipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChipViewHolder holder, int position) {
        User user = members.get(position);
        holder.tvName.setText(user.getBestDisplayName());
        // Use ProfileImageHelper to show emoji avatar or initial
        ProfileImageHelper.loadProfileIntoTextView(user, holder.tvInitial);
        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) listener.onRemoveClick(user, pos);
        });
    }

    @Override
    public int getItemCount() { return members.size(); }

    static class ChipViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvInitial;
        final ImageButton btnRemove;

        ChipViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_member_chip_name);
            tvInitial = itemView.findViewById(R.id.tv_member_initial);
            btnRemove = itemView.findViewById(R.id.btn_remove_member);
        }
    }
}
