package com.example.demo.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.demo.data.GroupRepository;
import com.example.demo.model.Group;
import com.example.demo.model.GroupExpense;
import com.example.demo.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewModel for all group-related screens.
 * Wraps GroupRepository and exposes LiveData to fragments.
 * Also handles the settlement calculation logic.
 */
public class GroupViewModel extends AndroidViewModel {

    private final GroupRepository repository;

    // Shared state used by detail + settlement screens
    private String currentGroupId;
    private Group currentGroup;
    private List<User> currentMembers = new ArrayList<>();

    // Result LiveData for one-off operations
    private final MutableLiveData<String> operationError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();
    public GroupViewModel(@NonNull Application application) {
        super(application);
        repository = new GroupRepository();
    }

    // ===================================================================
    //  Groups
    // ===================================================================

    public LiveData<List<Group>> getUserGroups(String userId) {
        return repository.getUserGroups(userId);
    }

    public void createGroup(Group group) {
        repository.createGroup(group, new GroupRepository.OnCompleteListener<String>() {
            @Override
            public void onSuccess(String result) {
                currentGroupId = result;
                currentGroup = group;
                operationSuccess.postValue(true);
            }
            @Override
            public void onFailure(Exception e) {
                operationError.postValue(e.getMessage());
            }
        });
    }

    public void fetchGroupDetails(String groupId) {
        repository.getGroupById(groupId, new GroupRepository.OnCompleteListener<Group>() {
            @Override
            public void onSuccess(Group group) {
                if (group != null) {
                    currentGroup = group;
                    currentGroupId = groupId;
                    // Trigger member fetch
                    getGroupMembers(group.getMembers()).observeForever(users -> {
                        if (users != null) currentMembers = users;
                    });
                }
            }
            @Override
            public void onFailure(Exception e) {
                operationError.postValue("Failed to load group: " + e.getMessage());
            }
        });
    }

    public void resetOperationState() {
        operationSuccess.setValue(null);
        operationError.setValue(null);
    }

    public void leaveGroup(String groupId, String userId) {
        repository.leaveGroup(groupId, userId, new GroupRepository.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                operationSuccess.postValue(true);
            }
            @Override
            public void onFailure(Exception e) {
                operationError.postValue(e.getMessage());
            }
        });
    }

    // ===================================================================
    //  Expenses
    // ===================================================================

    public LiveData<List<GroupExpense>> getGroupExpenses(String groupId) {
        this.currentGroupId = groupId;
        return repository.getGroupExpenses(groupId);
    }

    public void addExpense(GroupExpense expense) {
        repository.addExpense(currentGroupId, expense, new GroupRepository.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                operationSuccess.postValue(true);
            }
            @Override
            public void onFailure(Exception e) {
                operationError.postValue(e.getMessage());
            }
        });
    }

    public void settleExpense(String groupId, String expenseId) {
        repository.settleExpense(groupId, expenseId, new GroupRepository.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                operationSuccess.postValue(true);
            }
            @Override
            public void onFailure(Exception e) {
                operationError.postValue(e.getMessage());
            }
        });
    }

    // ===================================================================
    //  Members
    // ===================================================================

    public LiveData<List<User>> getGroupMembers(List<String> memberIds) {
        return repository.getGroupMembers(memberIds);
    }

    public void findUserByEmail(String email, GroupRepository.OnCompleteListener<User> listener) {
        repository.findUserByEmail(email, listener);
    }

    // ===================================================================
    //  Settlement Calculation
    // ===================================================================

    /**
     * Compute simplified debts from a list of group expenses.
     * Uses a net-balance approach:
     *   1. For each expense: payer gets credited the total amount,
     *      each splitAmong member is debited their share.
     *   2. Balances are simplified into a minimal set of transactions.
     *
     * @param expenses   all expenses in the group
     * @param currentUid the viewing user's uid (filters to show only their debts)
     * @return list of Settlement objects describing who owes whom
     */
    public List<Settlement> computeSettlements(List<GroupExpense> expenses, String currentUid) {
        Map<String, Double> balance = new HashMap<>();

        for (GroupExpense expense : expenses) {
            if (expense.isSettled()) continue;
            List<String> splitAmong = expense.getSplitAmong();
            if (splitAmong == null || splitAmong.isEmpty()) continue;

            double share = expense.getAmount() / splitAmong.size();
            String payer = expense.getPaidBy();

            // Payer is credited the full amount
            balance.put(payer, balance.getOrDefault(payer, 0.0) + expense.getAmount());

            // Each member in splitAmong is debited their share
            for (String uid : splitAmong) {
                balance.put(uid, balance.getOrDefault(uid, 0.0) - share);
            }
        }

        // Build settlement list — only from/to currentUid perspective
        List<Settlement> settlements = new ArrayList<>();
        for (Map.Entry<String, Double> entry : balance.entrySet()) {
            String uid = entry.getKey();
            double amt = entry.getValue();
            if (uid.equals(currentUid)) continue;

            // currentUid's perspective:
            double myBalance = balance.getOrDefault(currentUid, 0.0);
            // Already handled all pairs below
        }

        // Simplified: list all non-trivial debts involving currentUid
        double myBalance = balance.getOrDefault(currentUid, 0.0);

        if (myBalance < -0.01) {
            // currentUid owes money — find who to pay
            // Simple approach: distribute what currentUid owes to those who are owed
            for (Map.Entry<String, Double> entry : balance.entrySet()) {
                String uid = entry.getKey();
                if (uid.equals(currentUid)) continue;
                double theirBalance = entry.getValue();
                if (theirBalance > 0.01) {
                    double payAmount = Math.min(-myBalance, theirBalance);
                    if (payAmount > 0.01) {
                        settlements.add(new Settlement(currentUid, uid, payAmount));
                    }
                }
            }
        } else if (myBalance > 0.01) {
            // currentUid is owed — list who owes them
            for (Map.Entry<String, Double> entry : balance.entrySet()) {
                String uid = entry.getKey();
                if (uid.equals(currentUid)) continue;
                double theirBalance = entry.getValue();
                if (theirBalance < -0.01) {
                    double receiveAmount = Math.min(myBalance, -theirBalance);
                    if (receiveAmount > 0.01) {
                        settlements.add(new Settlement(uid, currentUid, receiveAmount));
                    }
                }
            }
        }

        return settlements;
    }

    /**
     * Compute net balance for the current user in a group.
     * Positive = you are owed, Negative = you owe.
     */
    public double computeMyBalance(List<GroupExpense> expenses, String currentUid) {
        double balance = 0;
        for (GroupExpense expense : expenses) {
            if (expense.isSettled()) continue;
            List<String> splitAmong = expense.getSplitAmong();
            if (splitAmong == null || splitAmong.isEmpty()) continue;

            double share = expense.getAmount() / splitAmong.size();

            if (expense.getPaidBy().equals(currentUid)) {
                balance += expense.getAmount();
            }
            if (splitAmong.contains(currentUid)) {
                balance -= share;
            }
        }
        return balance;
    }

    // ===================================================================
    //  State helpers
    // ===================================================================

    public void setCurrentGroupId(String groupId) { this.currentGroupId = groupId; }
    public String getCurrentGroupId() { return currentGroupId; }

    public void setCurrentGroup(Group group) { this.currentGroup = group; }
    public Group getCurrentGroup() { return currentGroup; }

    public void setCurrentMembers(List<User> members) { this.currentMembers = members; }
    public List<User> getCurrentMembers() { return currentMembers; }

    public LiveData<String> getOperationError() { return operationError; }
    public LiveData<Boolean> getOperationSuccess() { return operationSuccess; }

    public void clearCurrentState() {
        currentGroupId = null;
        currentGroup = null;
        currentMembers.clear();
        resetOperationState();
    }

    // ===================================================================
    //  Cleanup
    // ===================================================================

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.removeListeners();
    }

    // ===================================================================
    //  Inner class: Settlement
    // ===================================================================

    public static class Settlement {
        public final String fromUserId; // who owes
        public final String toUserId;   // who is owed
        public final double amount;

        public Settlement(String fromUserId, String toUserId, double amount) {
            this.fromUserId = fromUserId;
            this.toUserId = toUserId;
            this.amount = amount;
        }
    }
}
