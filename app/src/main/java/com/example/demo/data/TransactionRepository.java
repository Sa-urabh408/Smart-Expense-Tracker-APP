package com.example.demo.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.demo.model.Transaction;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository class that abstracts access to the Room database.
 * All database operations are performed on a background thread.
 * Synchronizes data with Firebase Firestore for cloud backup.
 */
public class TransactionRepository {

    private final TransactionDao transactionDao;
    private final ExecutorService executorService;
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "transactions";

    public TransactionRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        transactionDao = database.transactionDao();
        executorService = Executors.newFixedThreadPool(4);
        db = FirebaseFirestore.getInstance();
    }

    // ==================== Insert / Update / Delete ====================

    /**
     * Insert a transaction locally and then sync to Firestore.
     */
    public void insert(Transaction transaction) {
        executorService.execute(() -> {
            // 1. Insert locally to get a Room ID (auto-generated)
            long localId = transactionDao.insert(transaction);
            transaction.setId((int) localId);

            // 2. Upload to Firestore
            db.collection(COLLECTION_NAME)
                    .add(transaction)
                    .addOnSuccessListener(documentReference -> {
                        // 3. Save Firestore ID back to local DB
                        transaction.setFirestoreId(documentReference.getId());
                        executorService.execute(() -> transactionDao.update(transaction));
                    });
        });
    }

    /**
     * Update a transaction locally and then sync to Firestore.
     */
    public void update(Transaction transaction) {
        executorService.execute(() -> {
            transactionDao.update(transaction);
            
            if (transaction.getFirestoreId() != null) {
                db.collection(COLLECTION_NAME)
                        .document(transaction.getFirestoreId())
                        .set(transaction);
            }
        });
    }

    /**
     * Delete a transaction locally and then sync to Firestore.
     */
    public void delete(Transaction transaction) {
        executorService.execute(() -> {
            transactionDao.delete(transaction);
            
            if (transaction.getFirestoreId() != null) {
                db.collection(COLLECTION_NAME)
                        .document(transaction.getFirestoreId())
                        .delete();
            }
        });
    }

    public void deleteById(int id) {
        // NOTE: Manual delete by ID doesn't easily sync if we don't have the firestoreId.
        // Usually better to pass the full Transaction object.
        executorService.execute(() -> transactionDao.deleteById(id));
    }

    // ==================== Queries ====================

    public LiveData<List<Transaction>> getAllTransactions(String userId) {
        return transactionDao.getAllTransactions(userId);
    }

    public List<Transaction> getAllTransactionsList(String userId) {
        return transactionDao.getAllTransactionsList(userId);
    }

    public LiveData<List<Transaction>> getRecentTransactions(String userId) {
        return transactionDao.getRecentTransactions(userId);
    }

    public LiveData<List<Transaction>> getTransactionsByType(String userId, String type) {
        return transactionDao.getTransactionsByType(userId, type);
    }

    public LiveData<List<Transaction>> getTransactionsByCategory(String userId, String category) {
        return transactionDao.getTransactionsByCategory(userId, category);
    }

    public LiveData<List<Transaction>> getTransactionsByDateRange(String userId, long startDate, long endDate) {
        return transactionDao.getTransactionsByDateRange(userId, startDate, endDate);
    }

    public LiveData<List<Transaction>> searchTransactions(String userId, String query) {
        return transactionDao.searchTransactions(userId, query);
    }

    // ==================== Aggregations ====================

    public LiveData<Double> getTotalIncome(String userId) {
        return transactionDao.getTotalIncome(userId);
    }

    public LiveData<Double> getTotalExpenses(String userId) {
        return transactionDao.getTotalExpenses(userId);
    }

    public LiveData<Double> getTotalByType(String userId, String type) {
        return transactionDao.getTotalByType(userId, type);
    }

    public LiveData<List<TransactionDao.CategorySum>> getCategoryExpenseSums(String userId) {
        return transactionDao.getCategoryExpenseSums(userId);
    }

    public LiveData<Integer> getTransactionCount(String userId) {
        return transactionDao.getTransactionCount(userId);
    }

    // ==================== Monthly Queries ====================

    public LiveData<List<Transaction>> getMonthlyTransactions(String userId, long monthStart, long monthEnd) {
        return transactionDao.getMonthlyTransactions(userId, monthStart, monthEnd);
    }

    public LiveData<Double> getMonthlyIncome(String userId, long monthStart, long monthEnd) {
        return transactionDao.getMonthlyIncome(userId, monthStart, monthEnd);
    }

    public LiveData<Double> getMonthlyExpense(String userId, long monthStart, long monthEnd) {
        return transactionDao.getMonthlyExpense(userId, monthStart, monthEnd);
    }

    public LiveData<Double> getWeeklyExpense(String userId, long weekStart, long weekEnd) {
        return transactionDao.getWeeklyExpense(userId, weekStart, weekEnd);
    }

    // ==================== Cloud Sync ====================

    /**
     * Fetch all transactions from Firestore for the given user and save to local DB.
     * Use this on login or manual refresh.
     */
    public void syncFromFirestore(String userId) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    executorService.execute(() -> {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Transaction cloudTx = doc.toObject(Transaction.class);
                            cloudTx.setFirestoreId(doc.getId());
                            // Check if already exists locally (by firestoreId)
                            // This is a simplified sync logic
                            executorService.execute(() -> {
                                // For simplicity, we just insert if not present
                                // In a real app, you'd check timestamps
                                transactionDao.insert(cloudTx); 
                            });
                        }
                    });
                });
    }
}
