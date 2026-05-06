package com.example.demo.model;

/**
 * User entity to represent user-specific settings like monthly budget.
 * Also stores email and displayName for group member lookup.
 * Stored in Firestore under the "users" collection.
 *
 * Profile display priority:
 *   1. profilePhotoBase64 (photo uploaded from camera/gallery)
 *   2. profileAvatar      (emoji chosen from avatar grid)
 *   3. Default teal circle with first letter of name
 */
public class User {
    private String userId;
    private double monthlyBudget;
    private String email;              // used for group member search
    private String displayName;        // shown in group member lists
    private String profilePhotoBase64; // Base64-encoded JPEG ≤ 500 KB
    private String profileAvatar;      // emoji string e.g. "😎"

    public User() {
        // Required no-arg constructor for Firestore
    }

    public User(String userId, double monthlyBudget) {
        this.userId = userId;
        this.monthlyBudget = monthlyBudget;
    }

    // ----- Basic fields -----
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getMonthlyBudget() { return monthlyBudget; }
    public void setMonthlyBudget(double monthlyBudget) { this.monthlyBudget = monthlyBudget; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    // ----- Profile photo / avatar -----
    public String getProfilePhotoBase64() { return profilePhotoBase64; }
    public void setProfilePhotoBase64(String profilePhotoBase64) { this.profilePhotoBase64 = profilePhotoBase64; }

    public String getProfileAvatar() { return profileAvatar; }
    public void setProfileAvatar(String profileAvatar) { this.profileAvatar = profileAvatar; }

    /** True when a real photo is stored. */
    public boolean hasPhoto() {
        return profilePhotoBase64 != null && !profilePhotoBase64.isEmpty();
    }

    /** True when an emoji avatar is stored. */
    public boolean hasAvatar() {
        return profileAvatar != null && !profileAvatar.isEmpty();
    }

    // ----- Display helpers -----

    /** Returns displayName if available, otherwise first part of email, else userId */
    public String getBestDisplayName() {
        if (displayName != null && !displayName.isEmpty()) return displayName;
        if (email != null && !email.isEmpty()) return email.split("@")[0];
        return userId != null ? userId.substring(0, Math.min(8, userId.length())) : "?";
    }

    /** Returns the first letter of the display name (for avatar initials) */
    public String getInitial() {
        String name = getBestDisplayName();
        return name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase();
    }
}
