package com.example.demo.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.demo.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository for managing User data (like monthly budget) in Firestore.
 */
public class UserRepository {
    
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "users";

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Set the monthly budget for the given user in Firestore.
     * Uses .update() to only change the budget field without overwriting
     * other fields like displayName and email.
     */
    public void setMonthlyBudget(String userId, double budget) {
        // First check if doc exists — if not, create with merge; if yes, just update
        Map<String, Object> data = new HashMap<>();
        data.put("monthlyBudget", budget);
        db.collection(COLLECTION_NAME).document(userId)
                .set(data, SetOptions.merge());
    }

    /**
     * Get the user's monthly budget from Firestore.
     * Returns a LiveData that updates asynchronously.
     */
    public LiveData<Double> getMonthlyBudget(String userId) {
        MutableLiveData<Double> budgetLiveData = new MutableLiveData<>(0.0);
        
        db.collection(COLLECTION_NAME).document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        budgetLiveData.postValue(user.getMonthlyBudget());
                    }
                }
            })
            .addOnFailureListener(e -> {
                budgetLiveData.postValue(0.0);
            });
            
        return budgetLiveData;
    }

    /**
     * Ensures the user's Firestore doc has displayName and email fields.
     * Call this on every login to repair any previously corrupted documents
     * (e.g., where setMonthlyBudget overwrote the entire doc).
     */
    public void ensureProfileData(String userId, String displayName, String email) {
        if (userId == null) return;

        Map<String, Object> data = new HashMap<>();
        if (displayName != null && !displayName.isEmpty()) {
            data.put("displayName", displayName);
        }
        if (email != null && !email.isEmpty()) {
            data.put("email", email.toLowerCase().trim());
        }
        data.put("userId", userId);

        if (!data.isEmpty()) {
            db.collection(COLLECTION_NAME).document(userId)
                    .set(data, SetOptions.merge());
        }
    }
}
