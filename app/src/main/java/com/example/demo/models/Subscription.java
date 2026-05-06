package com.example.demo.models;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * Subscription model for the Subscription Tracker feature.
 *
 * Fields:
 *   name         – Service name (e.g. "Netflix")
 *   emoji        – Display icon (e.g. "🎬")
 *   price        – Cost per cycle (e.g. 649.0)
 *   cycle        – MONTHLY | WEEKLY | YEARLY
 *   renewalDate  – Next renewal date
 *   category     – ENTERTAINMENT | PRODUCTIVITY | CLOUD | HEALTH | OTHER
 *   isActive     – Currently active
 *   isTrial      – Free trial
 *   notes        – User notes
 */
public class Subscription {

    // ── Enums ────────────────────────────────────────────────────────────────

    public enum BillingCycle {
        WEEKLY, MONTHLY, YEARLY
    }

    public enum Category {
        ENTERTAINMENT, PRODUCTIVITY, CLOUD, HEALTH, OTHER
    }

    public enum Urgency {
        URGENT,   // ≤3 days  (red)
        WARNING,  // 4-7 days (amber)
        SAFE      // 8+ days  (green)
    }

    // ── Fields ───────────────────────────────────────────────────────────────

    private String id;
    private String name;
    private String emoji;
    private double price;
    private BillingCycle cycle;
    private long renewalDateMillis; // stored as epoch millis
    private Category category;
    private boolean isActive;
    private boolean isTrial;
    private String notes;

    // ── Constructors ─────────────────────────────────────────────────────────

    public Subscription() {
        this.id = UUID.randomUUID().toString();
        this.emoji = "📦";
        this.cycle = BillingCycle.MONTHLY;
        this.category = Category.OTHER;
        this.isActive = true;
        this.isTrial = false;
        this.notes = "";
    }

    public Subscription(String name, String emoji, double price,
                        BillingCycle cycle, long renewalDateMillis,
                        Category category) {
        this();
        this.name = name;
        this.emoji = emoji;
        this.price = price;
        this.cycle = cycle;
        this.renewalDateMillis = renewalDateMillis;
        this.category = category;
    }

    // ── Smart computed methods ────────────────────────────────────────────────

    /**
     * Days left until renewal. Returns 0 if renewal is today or past.
     */
    public long daysLeft() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        long diff = renewalDateMillis - today.getTimeInMillis();
        long days = diff / (1000L * 60 * 60 * 24);
        return Math.max(days, 0);
    }

    /**
     * Effective monthly cost regardless of billing cycle.
     */
    public double monthlyPrice() {
        if (cycle == null) return price;
        switch (cycle) {
            case YEARLY:  return price / 12.0;
            case WEEKLY:  return price * 4.33;
            default:      return price;
        }
    }

    /**
     * Urgency level based on days left until renewal.
     */
    public Urgency urgency() {
        long days = daysLeft();
        if (days <= 3) return Urgency.URGENT;
        if (days <= 7) return Urgency.WARNING;
        return Urgency.SAFE;
    }

    /**
     * Returns the renewal date as a Date object.
     */
    public Date getRenewalDate() {
        return new Date(renewalDateMillis);
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getId()                          { return id; }
    public void setId(String id)                   { this.id = id; }

    public String getName()                        { return name; }
    public void setName(String name)               { this.name = name; }

    public String getEmoji()                       { return emoji; }
    public void setEmoji(String emoji)             { this.emoji = emoji; }

    public double getPrice()                       { return price; }
    public void setPrice(double price)             { this.price = price; }

    public BillingCycle getCycle()                  { return cycle; }
    public void setCycle(BillingCycle cycle)        { this.cycle = cycle; }

    public long getRenewalDateMillis()              { return renewalDateMillis; }
    public void setRenewalDateMillis(long millis)   { this.renewalDateMillis = millis; }

    public Category getCategory()                  { return category; }
    public void setCategory(Category category)     { this.category = category; }

    public boolean isActive()                      { return isActive; }
    public void setActive(boolean active)          { this.isActive = active; }

    public boolean isTrial()                       { return isTrial; }
    public void setTrial(boolean trial)            { this.isTrial = trial; }

    public String getNotes()                       { return notes; }
    public void setNotes(String notes)             { this.notes = notes; }
}
