package com.example.demo.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.demo.model.Transaction;

import java.util.List;

/**
 * Data Access Object for Transaction entity.
 * Provides all database query methods for transaction operations.
 */
@Dao
public interface TransactionDao {

    // Insert a new transaction
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    long insert(Transaction transaction);

    // Update an existing transaction
    @Update
    void update(Transaction transaction);

    // Delete a transaction
    @Delete
    void delete(Transaction transaction);

    // Delete transaction by ID
    @Query("DELETE FROM transactions WHERE id = :id")
    void deleteById(int id);

    // Get all transactions for a user, ordered by date descending
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    LiveData<List<Transaction>> getAllTransactions(String userId);

    // Get all transactions as a plain list (for PDF export)
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    List<Transaction> getAllTransactionsList(String userId);

    // Get recent transactions (last 5)
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC LIMIT 5")
    LiveData<List<Transaction>> getRecentTransactions(String userId);

    // Get transactions by type (income/expense)
    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = :type ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByType(String userId, String type);

    // Get transactions by category
    @Query("SELECT * FROM transactions WHERE userId = :userId AND category = :category ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByCategory(String userId, String category);

    // Get transactions within a date range
    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByDateRange(String userId, long startDate, long endDate);

    // Search transactions by title
    @Query("SELECT * FROM transactions WHERE userId = :userId AND title LIKE '%' || :query || '%' ORDER BY date DESC")
    LiveData<List<Transaction>> searchTransactions(String userId, String query);

    // Get total income
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE userId = :userId AND type = 'income'")
    LiveData<Double> getTotalIncome(String userId);

    // Get total expenses
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE userId = :userId AND type = 'expense'")
    LiveData<Double> getTotalExpenses(String userId);

    // Get total amount by type
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE userId = :userId AND type = :type")
    LiveData<Double> getTotalByType(String userId, String type);

    // Get sum by category for expense type
    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE userId = :userId AND type = 'expense' GROUP BY category")
    LiveData<List<CategorySum>> getCategoryExpenseSums(String userId);

    // Get transaction count for user
    @Query("SELECT COUNT(*) FROM transactions WHERE userId = :userId")
    LiveData<Integer> getTransactionCount(String userId);

    // Get monthly transactions
    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :monthStart AND :monthEnd ORDER BY date DESC")
    LiveData<List<Transaction>> getMonthlyTransactions(String userId, long monthStart, long monthEnd);

    // Get monthly income total
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE userId = :userId AND type = 'income' AND date BETWEEN :monthStart AND :monthEnd")
    LiveData<Double> getMonthlyIncome(String userId, long monthStart, long monthEnd);

    // Get monthly expense total
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE userId = :userId AND type = 'expense' AND date BETWEEN :monthStart AND :monthEnd")
    LiveData<Double> getMonthlyExpense(String userId, long monthStart, long monthEnd);

    // Get weekly expense total
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE userId = :userId AND type = 'expense' AND date BETWEEN :weekStart AND :weekEnd")
    LiveData<Double> getWeeklyExpense(String userId, long weekStart, long weekEnd);

    /**
     * Helper class for category sum queries.
     */
    class CategorySum {
        public String category;
        public double total;
    }
}
