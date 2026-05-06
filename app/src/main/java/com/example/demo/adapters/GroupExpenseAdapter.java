package com.example.demo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.R;
import com.example.demo.model.GroupExpense;
import com.example.demo.model.User;
import com.example.demo.utils.ProfileImageHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RecyclerView adapter for expenses within a group.
 */
public class GroupExpenseAdapter extends RecyclerView.Adapter<GroupExpenseAdapter.ExpenseViewHolder> {

    private List<GroupExpense> expenses = new ArrayList<>();
    private Map<String, User> membersMap;  // userId -> User
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());

    public void setExpenses(List<GroupExpense> expenses) {
        this.expenses = expenses != null ? expenses : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setMembersMap(Map<String, User> membersMap) {
        this.membersMap = membersMap;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        GroupExpense expense = expenses.get(position);
        holder.bind(expense, membersMap, dateFormat);
    }

    @Override
    public int getItemCount() { return expenses.size(); }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDescription;
        private final TextView tvAmount;
        private final TextView tvPaidBy;
        private final TextView tvDate;
        private final TextView tvSettledBadge;
        private final TextView tvPayerAvatar;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tv_expense_description);
            tvAmount = itemView.findViewById(R.id.tv_expense_amount);
            tvPaidBy = itemView.findViewById(R.id.tv_expense_paid_by);
            tvDate = itemView.findViewById(R.id.tv_expense_date);
            tvSettledBadge = itemView.findViewById(R.id.tv_expense_settled_badge);
            tvPayerAvatar = itemView.findViewById(R.id.tv_payer_avatar);
        }

        void bind(GroupExpense expense, Map<String, User> membersMap, SimpleDateFormat dateFormat) {
            tvDescription.setText(expense.getDescription());
            tvAmount.setText(String.format(Locale.getDefault(), "₹%.2f", expense.getAmount()));
            tvDate.setText(dateFormat.format(new Date(expense.getDate())));

            String payerName = expense.getPaidBy();
            User payerUser = null;
            if (membersMap != null && membersMap.containsKey(expense.getPaidBy())) {
                payerUser = membersMap.get(expense.getPaidBy());
                payerName = payerUser.getBestDisplayName();
            }
            tvPaidBy.setText("Paid by " + payerName);

            // Show payer avatar (emoji or initial)
            if (tvPayerAvatar != null) {
                ProfileImageHelper.loadProfileIntoTextView(payerUser, tvPayerAvatar);
            }

            if (expense.isSettled()) {
                tvSettledBadge.setVisibility(View.VISIBLE);
                itemView.setAlpha(0.6f);
            } else {
                tvSettledBadge.setVisibility(View.GONE);
                itemView.setAlpha(1.0f);
            }
        }
    }
}
