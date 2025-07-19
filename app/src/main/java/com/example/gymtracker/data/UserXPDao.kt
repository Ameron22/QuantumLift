package com.example.gymtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserXPDao {
    // UserXP methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserXP(userXP: UserXP)

    @Update
    suspend fun updateUserXP(userXP: UserXP)

    @Query("SELECT * FROM user_xp WHERE userId = :userId")
    suspend fun getUserXP(userId: String): UserXP?

    @Query("SELECT * FROM user_xp WHERE userId = :userId")
    fun getUserXPFlow(userId: String): Flow<UserXP?>

    // XPHistory methods
    @Insert
    suspend fun insertXPHistory(xpHistory: XPHistory): Long

    @Query("SELECT * FROM xp_history WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentXPHistory(userId: String, limit: Int = 10): List<XPHistory>

    @Query("SELECT * FROM xp_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getXPHistoryFlow(userId: String): Flow<List<XPHistory>>

    @Query("SELECT SUM(xpEarned) FROM xp_history WHERE userId = :userId")
    suspend fun getTotalXPEarned(userId: String): Int?

    // Debug methods
    @Query("SELECT name FROM sqlite_master WHERE type='table' AND name='user_xp'")
    suspend fun checkUserXPTableExists(): String?

    @Query("SELECT name FROM sqlite_master WHERE type='table' AND name='xp_history'")
    suspend fun checkXPHistoryTableExists(): String?
} 