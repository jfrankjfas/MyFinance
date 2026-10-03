package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ArchivedPeriodDao
import com.example.data.dao.BudgetDao
import com.example.data.dao.ExtraordinaryFundDao
import com.example.data.dao.ScheduledExpenseDao
import com.example.data.dao.TransactionDao
import com.example.data.entity.ArchivedPeriodEntity
import com.example.data.entity.BudgetEntity
import com.example.data.entity.ExtraordinaryFundEntity
import com.example.data.entity.ScheduledExpenseEntity
import com.example.data.entity.TransactionEntity

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE scheduled_expenses ADD COLUMN fundingSource TEXT NOT NULL DEFAULT 'PERIOD_BUDGET'")
        db.execSQL("ALTER TABLE scheduled_expenses ADD COLUMN extraordinaryFundId INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE scheduled_expenses ADD COLUMN extraordinaryFundTitle TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [TransactionEntity::class, BudgetEntity::class, ScheduledExpenseEntity::class, ArchivedPeriodEntity::class, ExtraordinaryFundEntity::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun scheduledExpenseDao(): ScheduledExpenseDao
    abstract fun archivedPeriodDao(): ArchivedPeriodDao
    abstract fun extraordinaryFundDao(): ExtraordinaryFundDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finanzas_clara_db"
                )
                    .addMigrations(MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
