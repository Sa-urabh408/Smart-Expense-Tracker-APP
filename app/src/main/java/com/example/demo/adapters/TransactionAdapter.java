package com.example.demo.adapters;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.R;
import com.example.demo.model.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying transaction items.
 * Supports click and long-click listeners for edit/delete functionality.
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactions = new ArrayList<>();
    private final OnTransactionClickListener listener;
    private final NumberFormat currencyFormat;
    private final SimpleDateFormat dateFormat;

    /**
     * Interface for handling transaction item interactions.
     */
    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
        void onTransactionLongClick(Transaction transaction, int position);
    }

    public TransactionAdapter(OnTransactionClickListener listener) {
        this.listener = listener;
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        Context context = holder.itemView.getContext();

        // Set title
        holder.tvTitle.setText(transaction.getTitle());

        // Set category
        holder.tvCategory.setText(transaction.getCategory());

        // Set date
        holder.tvDate.setText(dateFormat.format(new Date(transaction.getDate())));

        // Set wallet type
        if (transaction.getWalletType() != null && !transaction.getWalletType().isEmpty()) {
            holder.tvWallet.setVisibility(View.VISIBLE);
            holder.tvWallet.setText(transaction.getWalletType());
        } else {
            holder.tvWallet.setVisibility(View.GONE);
        }

        // Set amount with color
        String amountText;
        if ("income".equals(transaction.getType())) {
            amountText = "+ " + currencyFormat.format(transaction.getAmount());
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.colorIncome));
        } else {
            amountText = "- " + currencyFormat.format(transaction.getAmount());
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.colorExpense));
        }
        holder.tvAmount.setText(amountText);

        // Set category icon and color
        setCategoryIcon(holder, transaction.getCategory(), context);

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTransactionClick(transaction);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onTransactionLongClick(transaction, holder.getAdapterPosition());
            }
            return true;
        });
    }

    /**
     * Set category-specific icon and background color.
     */
    private void setCategoryIcon(TransactionViewHolder holder, String category, Context context) {
        int iconRes;
        int colorRes;

        switch (category) {
            case "Food":
                iconRes = R.drawable.ic_food;
                colorRes = R.color.categoryFood;
                break;
            case "Travel":
                iconRes = R.drawable.ic_travel;
                colorRes = R.color.categoryTravel;
                break;
            case "Shopping":
                iconRes = R.drawable.ic_shopping;
                colorRes = R.color.categoryShopping;
                break;
            case "Bills":
                iconRes = R.drawable.ic_bills;
                colorRes = R.color.categoryBills;
                break;
            case "Entertainment":
                iconRes = R.drawable.ic_entertainment;
                colorRes = R.color.categoryEntertainment;
                break;
            case "Health":
                iconRes = R.drawable.ic_health;
                colorRes = R.color.categoryHealth;
                break;
            case "Salary":
                iconRes = R.drawable.ic_salary;
                colorRes = R.color.categoryIncome;
                break;
            default:
                iconRes = R.drawable.ic_star;
                colorRes = R.color.categoryOther;
                break;
        }

        holder.ivCategoryIcon.setImageResource(iconRes);

        // Set circular background color
        GradientDrawable bgDrawable = new GradientDrawable();
        bgDrawable.setShape(GradientDrawable.OVAL);
        bgDrawable.setColor(ContextCompat.getColor(context, colorRes));
        holder.viewCategoryBg.setBackground(bgDrawable);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    /**
     * Update the transaction list and refresh the adapter.
     */
    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Remove a transaction at the given position.
     */
    public Transaction removeItem(int position) {
        if (position >= 0 && position < transactions.size()) {
            Transaction removed = transactions.remove(position);
            notifyItemRemoved(position);
            return removed;
        }
        return null;
    }

    /**
     * Restore a previously removed transaction.
     */
    public void restoreItem(Transaction transaction, int position) {
        transactions.add(position, transaction);
        notifyItemInserted(position);
    }

    /**
     * ViewHolder for transaction items.
     */
    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvCategory, tvDate, tvAmount, tvWallet;
        final ImageView ivCategoryIcon;
        final View viewCategoryBg;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvWallet = itemView.findViewById(R.id.tvWallet);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            viewCategoryBg = itemView.findViewById(R.id.viewCategoryBg);
        }
    }
}
