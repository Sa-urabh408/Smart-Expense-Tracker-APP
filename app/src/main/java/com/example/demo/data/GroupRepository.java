package com.example.demo.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.demo.model.Group;
import com.example.demo.model.GroupExpense;
import com.example.demo.model.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for all group-related data.
 * Uses Firestore exclusively (no Room) since group data is shared across multiple users.
 * Real-time updates are delivered via Firestore snapshot listeners wrapped in LiveData.
 */
public class GroupRepository {

    private static final String GROUPS_COLLECTION = "groups";
    private static final String EXPENSES_SUBCOLLECTION = "expenses";
    private static final String USERS_COLLECTION = "users";

    private final FirebaseFirestore db;

    // Active listeners — call remove() when ViewModel is cleared to avoid leaks
    private ListenerRegistration groupsListener;
    private ListenerRegistration expensesListener;

    public GroupRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // ===================================================================
    //  Groups CRUD
    // ===================================================================

    /**
     * Create a new group in Firestore and set its generated ID back on the object.
     */
    public void createGroup(Group group, OnCompleteListener<String> listener) {
        // Generate document reference with ID first to be atomic
        com.google.firebase.firestore.DocumentReference ref = db.collection(GROUPS_COLLECTION).document();
        String groupId = ref.getId();
        group.setGroupId(groupId);

        ref.set(group)
                .addOnSuccessListener(aVoid -> listener.onSuccess(groupId))
                .addOnFailureListener(listener::onFailure);
    }

    /**
     * Returns a LiveData that receives real-time updates of all groups
     * where the given userId is in the members array.
     */
    public LiveData<List<Group>> getUserGroups(String userId) {
        MutableLiveData<List<Group>> liveData = new MutableLiveData<>();

        // Remove previous listener if any
        if (groupsListener != null) groupsListener.remove();

        groupsListener = db.collection(GROUPS_COLLECTION)
                .whereArrayContains("members", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    List<Group> groups = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Group g = doc.toObject(Group.class);
                        g.setGroupId(doc.getId());
                        groups.add(g);
                    }
                    liveData.postValue(groups);
                });

        return liveData;
    }

    /**
     * Fetch a single group by ID (one-time read).
     */
    public void getGroupById(String groupId, OnCompleteListener<Group> listener) {
        db.collection(GROUPS_COLLECTION).document(groupId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Group g = doc.toObject(Group.class);
                        if (g != null) g.setGroupId(doc.getId());
                        listener.onSuccess(g);
                    } else {
                        listener.onSuccess(null);
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    /**
     * Remove a user from a group's members list.
     */
    public void leaveGroup(String groupId, String userId, OnCompleteListener<Void> listener) {
        db.collection(GROUPS_COLLECTION).document(groupId)
                .update("members", FieldValue.arrayRemove(userId))
                .addOnSuccessListener(listener::onSuccess)
                .addOnFailureListener(listener::onFailure);
    }

    // ===================================================================
    //  Expenses CRUD
    // ===================================================================

    /**
     * Add an expense to a group's expenses subcollection.
     */
    public void addExpense(String groupId, GroupExpense expense, OnCompleteListener<Void> listener) {
        db.collection(GROUPS_COLLECTION).document(groupId)
                .collection(EXPENSES_SUBCOLLECTION)
                .add(expense)
                .addOnSuccessListener(ref -> {
                    // We don't really need to write the ID back into the document
                    // because we set it when reading (doc.getId()).
                    // This avoids a redundant update and potential listener glitches.
                    listener.onSuccess(null);
                })
                .addOnFailureListener(listener::onFailure);
    }

    /**
     * Returns a LiveData with real-time updates for all expenses in a group.
     * Performs sorting locally to avoid complex Firestore index requirements.
     */
    public LiveData<List<GroupExpense>> getGroupExpenses(String groupId) {
        return new MutableLiveData<List<GroupExpense>>() {
            private ListenerRegistration listener;

            @Override
            protected void onActive() {
                super.onActive();
                if (groupId == null || groupId.isEmpty()) {
                    postValue(new ArrayList<>());
                    return;
                }
                listener = db.collection(GROUPS_COLLECTION).document(groupId)
                        .collection(EXPENSES_SUBCOLLECTION)
                        .addSnapshotListener((snapshots, error) -> {
                            if (error != null) {
                                postValue(new ArrayList<>());
                                return;
                            }
                            if (snapshots == null) return;

                            List<GroupExpense> expenses = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : snapshots) {
                                GroupExpense e = doc.toObject(GroupExpense.class);
                                if (e != null) {
                                    e.setExpenseId(doc.getId());
                                    expenses.add(e);
                                }
                            }
                            expenses.sort((e1, e2) -> Long.compare(e2.getDate(), e1.getDate()));
                            postValue(expenses);
                        });
            }

            @Override
            protected void onInactive() {
                super.onInactive();
                if (listener != null) {
                    listener.remove();
                    listener = null;
                }
            }
        };
    }

    /**
     * Mark an expense as settled in Firestore.
     */
    public void settleExpense(String groupId, String expenseId, OnCompleteListener<Void> listener) {
        db.collection(GROUPS_COLLECTION).document(groupId)
                .collection(EXPENSES_SUBCOLLECTION).document(expenseId)
                .update("settled", true)
                .addOnSuccessListener(listener::onSuccess)
                .addOnFailureListener(listener::onFailure);
    }

    // ===================================================================
    //  User Lookup
    // ===================================================================

    /**
     * Find a registered user by email address (for adding members to a group).
     * Requires users to have a document in the "users" collection with an "email" field.
     */
    public void findUserByEmail(String email, OnCompleteListener<User> listener) {
        db.collection(USERS_COLLECTION)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        listener.onSuccess(null);
                    } else {
                        DocumentSnapshot doc = snapshots.getDocuments().get(0);
                        User user = doc.toObject(User.class);
                        if (user != null) user.setUserId(doc.getId());
                        listener.onSuccess(user);
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    /**
     * Batch-fetch user display info for a list of userIds.
     * Returns a LiveData list of User objects.
     */
    public LiveData<List<User>> getGroupMembers(List<String> memberIds) {
        MutableLiveData<List<User>> liveData = new MutableLiveData<>();
        List<User> result = new ArrayList<>();

        if (memberIds == null || memberIds.isEmpty()) {
            liveData.postValue(result);
            return liveData;
        }

        // Firestore "in" queries support up to 10 values — fine for typical group sizes
        List<String> ids = memberIds.size() > 10 ? memberIds.subList(0, 10) : memberIds;

        // Use FieldPath.documentId() to query by actual document IDs, not a field
        db.collection(USERS_COLLECTION)
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), ids)
                .get()
                .addOnSuccessListener(snapshots -> {
                    java.util.Set<String> foundIds = new java.util.HashSet<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            u.setUserId(doc.getId());
                            // Fallback: if displayName is empty, try FirebaseAuth display name
                            if ((u.getDisplayName() == null || u.getDisplayName().isEmpty())
                                    && u.getEmail() != null && !u.getEmail().isEmpty()) {
                                u.setDisplayName(u.getEmail().split("@")[0]);
                            }
                            result.add(u);
                            foundIds.add(doc.getId());
                        }
                    }

                    // Create placeholder users for any member IDs not found in Firestore
                    for (String id : ids) {
                        if (!foundIds.contains(id)) {
                            User placeholder = new User();
                            placeholder.setUserId(id);
                            placeholder.setDisplayName("User " + id.substring(0, Math.min(4, id.length())));
                            result.add(placeholder);
                        }
                    }

                    liveData.postValue(result);
                })
                .addOnFailureListener(e -> liveData.postValue(result));

        return liveData;
    }

    // ===================================================================
    //  Cleanup
    // ===================================================================

    /** Call this from ViewModel.onCleared() to release Firestore listeners. */
    public void removeListeners() {
        if (groupsListener != null) {
            groupsListener.remove();
            groupsListener = null;
        }
        if (expensesListener != null) {
            expensesListener.remove();
            expensesListener = null;
        }
    }

    // ===================================================================
    //  Callback interfaces
    // ===================================================================

    public interface OnCompleteListener<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }
}
