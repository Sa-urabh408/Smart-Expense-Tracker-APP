package com.example.demo.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Transaction entity representing a single income or expense transaction.
 * Stored in the Room database's "transactions" table.
 */
@Entity(tableName = "transactions")
public class Transaction {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String firestoreId; // Unique ID for Firestore document
    private String userId; // Firebase user ID
    private String title; // Transaction title/description
    private double amount; // Transaction amount
    private String category; // Category (Food, Travel, Shopping, etc.)
    private String type; // "income" or "expense"
    private long date; // Date stored as timestamp (milliseconds)
    private String walletType; // "Cash", "Bank", "UPI", "Credit Card"
    private String notes; // Optional notes

    // No-arg constructor required for Firestore
    public Transaction() {
    }

    // Constructor
    public Transaction(String userId, String title, double amount, String category,
            String type, long date, String walletType, String notes) {
        this.userId = userId;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.type = type;
        this.date = date;
        this.walletType = walletType;
        this.notes = notes;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirestoreId() {
        return firestoreId;
    }

    public void setFirestoreId(String firestoreId) {
        this.firestoreId = firestoreId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getWalletType() {
        return walletType;
    }

    public void setWalletType(String walletType) {
        this.walletType = walletType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
