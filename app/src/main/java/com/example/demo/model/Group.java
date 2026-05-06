package com.example.demo.model;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a shared expense group.
 * Stored in Firestore: groups/{groupId}
 */
public class Group {

    private String groupId;
    private String name;
    private String createdBy;   // userId of creator
    private List<String> members; // list of userIds
    private long createdAt;

    // No-arg constructor required by Firestore
    public Group() {
        members = new ArrayList<>();
    }

    public Group(String name, String createdBy, List<String> members) {
        this.name = name;
        this.createdBy = createdBy;
        this.members = members != null ? members : new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    // ==================== Helpers ====================

    @Exclude
    public boolean isMember(String userId) {
        return members != null && members.contains(userId);
    }

    // ==================== Getters / Setters ====================

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
