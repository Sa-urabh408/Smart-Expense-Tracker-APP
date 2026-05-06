package com.example.demo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.R;
import com.example.demo.models.Subscription;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for the dedicated Subscription screen.
 * Shows subscription cards with urgency-colored badges.
 */
public class SubscriptionAdapter
        extends RecyclerView.Adapter<SubscriptionAdapter.ViewHolder> {

    public interface OnSubscriptionClickListener {
        void onSubscriptionClick(Subscription sub);
        void onSubscriptionLongClick(Subscription sub, int position);
    }

    private final Context context;
    private List<Subscription> subscriptions = new ArrayList<>();
    private final OnSubscriptionClickListener listener;
    private final SimpleDateFormat dateFmt =
            new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

    public SubscriptionAdapter(Context context, OnSubscriptionClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setSubscriptions(List<Subscription> list) {
        this.subscriptions = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_subscription, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Subscription sub = subscriptions.get(pos);

        // Emoji
        h.tvEmoji.setText(sub.getEmoji() != null ? sub.getEmoji() : "📦");

        // Name
        h.tvName.setText(sub.getName());

        // Category
        String catDisplay = sub.getCategory() != null
                ? capitalize(sub.getCategory().name()) : "Other";
        h.tvCategory.setText(catDisplay);

        // Price
        h.tvPrice.setText("₹" + formatIndian(sub.getPrice()));

        // Cycle
        String cycleText = "/month";
        if (sub.getCycle() != null) {
            switch (sub.getCycle()) {
                case WEEKLY:  cycleText = "/week"; break;
                case YEARLY:  cycleText = "/year"; break;
                default:      cycleText = "/month"; break;
            }
        }
        h.tvCycle.setText(cycleText);

        // Urgency badge
        long days = sub.daysLeft();
        Subscription.Urgency urgency = sub.urgency();

        if (sub.isActive() && days <= 7) {
            h.tvBadge.setVisibility(View.VISIBLE);

            String badgeText;
            if (days <= 1) {
                badgeText = "⚡ Kal renew!";
            } else {
                badgeText = "⚡ " + days + " din baaki";
            }
            h.tvBadge.setText(badgeText);

            switch (urgency) {
                case URGENT:
                    h.tvBadge.setBackgroundResource(R.drawable.bg_badge_red);
                    h.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.subRedAlert));
                    break;
                case WARNING:
                    h.tvBadge.setBackgroundResource(R.drawable.bg_badge_amber);
                    h.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.subAmber));
                    break;
                default:
                    h.tvBadge.setBackgroundResource(R.drawable.bg_badge_green_sub);
                    h.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.subGreenSafe));
                    break;
            }
        } else if (sub.isActive()) {
            h.tvBadge.setVisibility(View.VISIBLE);
            h.tvBadge.setText("🟢 " + days + " din baaki");
            h.tvBadge.setBackgroundResource(R.drawable.bg_badge_green_sub);
            h.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.subGreenSafe));
        } else {
            h.tvBadge.setVisibility(View.GONE);
        }

        // Icon box color per category
        if (sub.getCategory() != null) {
            int bgRes;
            switch (sub.getCategory()) {
                case ENTERTAINMENT: bgRes = R.drawable.bg_badge_red; break;
                case HEALTH:        bgRes = R.drawable.bg_badge_green_sub; break;
                case CLOUD:         bgRes = R.drawable.bg_badge_amber; break;
                default:            bgRes = R.drawable.bg_icon_box; break;
            }
            h.tvEmoji.setBackgroundResource(bgRes);
        }

        // Click listeners
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSubscriptionClick(sub);
        });
        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onSubscriptionLongClick(sub, pos);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return subscriptions.size();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    private String formatIndian(double amount) {
        long value = (long) amount;
        if (value < 1000) return String.valueOf(value);

        String s = String.valueOf(value);
        int len = s.length();
        StringBuilder result = new StringBuilder();
        result.insert(0, s.substring(len - 3));
        int remaining = len - 3;
        while (remaining > 0) {
            int start = Math.max(0, remaining - 2);
            result.insert(0, s.substring(start, remaining) + ",");
            remaining = start;
        }
        return result.toString();
    }

    // ── ViewHolder ──────────────────────────────────────────────────────────

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvName, tvCategory, tvBadge, tvPrice, tvCycle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji    = itemView.findViewById(R.id.tvSubEmoji);
            tvName     = itemView.findViewById(R.id.tvSubName);
            tvCategory = itemView.findViewById(R.id.tvSubCategory);
            tvBadge    = itemView.findViewById(R.id.tvSubBadge);
            tvPrice    = itemView.findViewById(R.id.tvSubPrice);
            tvCycle    = itemView.findViewById(R.id.tvSubCycle);
        }
    }
}
