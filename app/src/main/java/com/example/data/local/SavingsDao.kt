package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SavingsAccountEntity
import com.example.data.model.SavingsTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {
    @Query("SELECT * FROM savings_account WHERE id = 1")
    fun getAccount(): Flow<SavingsAccountEntity?>

    @Query("SELECT * FROM savings_account WHERE id = 1")
    suspend fun getAccountSync(): SavingsAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAccount(account: SavingsAccountEntity)

    @Query("SELECT * FROM savings_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<SavingsTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: SavingsTransactionEntity): Long

    @Query("DELETE FROM savings_transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM savings_account")
    suspend fun deleteAccount()
}
