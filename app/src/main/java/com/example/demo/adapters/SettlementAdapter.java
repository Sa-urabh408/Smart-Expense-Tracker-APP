package com.example.demo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.R;
import com.example.demo.model.User;
import com.example.demo.viewmodel.GroupViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RecyclerView adapter for the settlement/debt list.
 * Shows who owes whom and allows marking as settled.
 */
public class SettlementAdapter extends RecyclerView.Adapter<SettlementAdapter.SettlementViewHolder> {

    public interface OnSettleClickListener {
        void onSettleClick(GroupViewModel.Settlement settlement, int position);
    }

    private List<GroupViewModel.Settlement> settlements = new ArrayList<>();
    private Map<String, User> membersMap;
    private String currentUserId;
    private final OnSettleClickListener listener;

    public SettlementAdapter(OnSettleClickListener listener, String currentUserId) {
        this.listener = listener;
        this.currentUserId = currentUserId;
    }

    public void setSettlements(List<GroupViewModel.Settlement> settlements) {
        this.settlements = settlements != null ? settlements : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setMembersMap(Map<String, User> membersMap) {
        this.membersMap = membersMap;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SettlementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_settlement, parent, false);
        return new SettlementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SettlementViewHolder holder, int position) {
        holder.bind(settlements.get(position), membersMap, currentUserId, listener, position);
    }

    @Override
    public int getItemCount() { return settlements.size(); }

    static class SettlementViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDebtDescription;
        private final TextView tvAmount;
        private final Button btnSettle;

        SettlementViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDebtDescription = itemView.findViewById(R.id.tv_settlement_description);
            tvAmount = itemView.findViewById(R.id.tv_settlement_amount);
            btnSettle = itemView.findViewById(R.id.btn_settle);
        }

        void bind(GroupViewModel.Settlement s, Map<String, User> membersMap,
                  String currentUserId, OnSettleClickListener listener, int position) {

            String fromName = getUserName(s.fromUserId, membersMap, currentUserId);
            String toName = getUserName(s.toUserId, membersMap, currentUserId);
            tvDebtDescription.setText(fromName + " owes " + toName);
            tvAmount.setText(String.format(Locale.getDefault(), "₹%.2f", s.amount));

            // Only show settle button if current user is involved
            boolean canSettle = s.fromUserId.equals(currentUserId) || s.toUserId.equals(currentUserId);
            btnSettle.setVisibility(canSettle ? View.VISIBLE : View.GONE);
            btnSettle.setOnClickListener(v -> listener.onSettleClick(s, position));
        }

        private String getUserName(String userId, Map<String, User> membersMap, String currentUserId) {
            if (userId.equals(currentUserId)) return "You";
            if (membersMap != null && membersMap.containsKey(userId)) {
                return membersMap.get(userId).getBestDisplayName();
            }
            return userId.substring(0, Math.min(8, userId.length()));
        }
    }
}
