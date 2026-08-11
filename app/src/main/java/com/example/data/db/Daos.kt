package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE referCode = :referCode LIMIT 1")
    suspend fun getUserByReferCode(referCode: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserByIdFlow(userId: Int): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET balance = balance + :amount WHERE id = :userId")
    suspend fun addToBalance(userId: Int, amount: Double)

    @Query("UPDATE users SET referredCount = referredCount + 1 WHERE id = :userId")
    suspend fun incrementReferredCount(userId: Int)

    @Query("SELECT * FROM users ORDER BY id DESC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Int): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Int)
}

@Dao
interface SubmissionDao {
    @Query("SELECT * FROM submissions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getSubmissionsForUserFlow(userId: Int): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submissions ORDER BY userId ASC, timestamp ASC")
    fun getAllSubmissionsFlow(): Flow<List<SubmissionEntity>>

    @Query("SELECT DISTINCT dateString FROM submissions ORDER BY dateString DESC")
    fun getSubmissionDatesFlow(): Flow<List<String>>

    @Query("SELECT * FROM submissions WHERE categoryId = :categoryId AND dateString = :dateString ORDER BY userId ASC, timestamp ASC")
    suspend fun getSubmissionsForCategoryAndDate(categoryId: Int, dateString: String): List<SubmissionEntity>

    @Query("SELECT * FROM submissions WHERE categoryId = :categoryId AND dateString = :dateString ORDER BY userId ASC, timestamp ASC")
    fun getSubmissionsForCategoryAndDateFlow(categoryId: Int, dateString: String): Flow<List<SubmissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmissions(submissions: List<SubmissionEntity>)

    @Query("UPDATE submissions SET status = :status WHERE id = :id")
    suspend fun updateSubmissionStatus(id: Int, status: String)

    @Query("SELECT * FROM submissions WHERE id = :id LIMIT 1")
    suspend fun getSubmissionById(id: Int): SubmissionEntity?
}

@Dao
interface WithdrawalDao {
    @Query("SELECT * FROM withdrawals WHERE userId = :userId ORDER BY timestamp DESC")
    fun getWithdrawalsForUserFlow(userId: Int): Flow<List<WithdrawalEntity>>

    @Query("SELECT * FROM withdrawals ORDER BY timestamp DESC")
    fun getAllWithdrawalsFlow(): Flow<List<WithdrawalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(withdrawal: WithdrawalEntity): Long

    @Query("UPDATE withdrawals SET status = :status WHERE id = :id")
    suspend fun updateWithdrawalStatus(id: Int, status: String)

    @Query("SELECT * FROM withdrawals WHERE id = :id LIMIT 1")
    suspend fun getWithdrawalById(id: Int): WithdrawalEntity?
}

@Dao
interface AdminConfigDao {
    @Query("SELECT * FROM admin_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<AdminConfigEntity?>

    @Query("SELECT * FROM admin_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): AdminConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: AdminConfigEntity)
}
