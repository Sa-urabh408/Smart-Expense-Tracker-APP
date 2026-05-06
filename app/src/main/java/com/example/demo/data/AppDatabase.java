package com.example.demo.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.demo.model.Transaction;

/**
 * Room Database for the Smart Expense Tracker app.
 * Singleton pattern ensures only one instance exists.
 */
@Database(entities = {Transaction.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    // Abstract method to get the DAO
    public abstract TransactionDao transactionDao();

    /**
     * Get singleton instance of the database.
     * Uses double-checked locking for thread safety.
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "expense_tracker_db"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
