package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ActivityLogEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SavingsAccountEntity
import com.example.data.model.SavingsTransactionEntity

@Database(
    entities = [
        ProductEntity::class,
        SavingsAccountEntity::class,
        SavingsTransactionEntity::class,
        ActivityLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BuySpaceDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun savingsDao(): SavingsDao
    abstract fun activityDao(): ActivityDao

    companion object {
        @Volatile
        private var INSTANCE: BuySpaceDatabase? = null

        fun getInstance(context: Context): BuySpaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BuySpaceDatabase::class.java,
                    "buyspace_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun getDatabase(context: Context): BuySpaceDatabase = getInstance(context)
    }
}
