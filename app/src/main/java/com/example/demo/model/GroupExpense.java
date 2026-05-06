package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single expense within a group.
 * Stored in Firestore: groups/{groupId}/expenses/{expenseId}
 */
public class GroupExpense {

    private String expenseId;
    private String groupId;
    private double amount;
    private String description;
    private String paidBy;             // userId of the payer
    private List<String> splitAmong;   // list of userIds sharing the cost
    private long date;
    private boolean settled;

    // No-arg constructor required by Firestore
    public GroupExpense() {
        splitAmong = new ArrayList<>();
    }

    public GroupExpense(String groupId, double amount, String description,
                        String paidBy, List<String> splitAmong) {
        this.groupId = groupId;
        this.amount = amount;
        this.description = description;
        this.paidBy = paidBy;
        this.splitAmong = splitAmong != null ? splitAmong : new ArrayList<>();
        this.date = System.currentTimeMillis();
        this.settled = false;
    }

    // ==================== Helpers ====================

    /** Per-person share amount */
    public double getSharePerPerson() {
        if (splitAmong == null || splitAmong.isEmpty()) return amount;
        return amount / splitAmong.size();
    }

    // ==================== Getters / Setters ====================

    public String getExpenseId() { return expenseId; }
    public void setExpenseId(String expenseId) { this.expenseId = expenseId; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPaidBy() { return paidBy; }
    public void setPaidBy(String paidBy) { this.paidBy = paidBy; }

    public List<String> getSplitAmong() { return splitAmong; }
    public void setSplitAmong(List<String> splitAmong) { this.splitAmong = splitAmong; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public boolean isSettled() { return settled; }
    public void setSettled(boolean settled) { this.settled = settled; }
}
