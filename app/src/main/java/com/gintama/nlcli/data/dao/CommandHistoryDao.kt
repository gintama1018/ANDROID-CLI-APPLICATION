package com.gintama.nlcli.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gintama.nlcli.data.entity.CommandHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CommandHistoryEntity): Long

    @Query("SELECT * FROM command_history ORDER BY timestampMs DESC")
    fun getAllHistory(): Flow<List<CommandHistoryEntity>>

    @Query("SELECT * FROM command_history ORDER BY timestampMs DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int = 20): List<CommandHistoryEntity>

    @Query("SELECT * FROM command_history WHERE success = :successOnly ORDER BY timestampMs DESC")
    fun getHistoryByStatus(successOnly: Boolean): Flow<List<CommandHistoryEntity>>

    @Query("DELETE FROM command_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM command_history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM command_history")
    suspend fun getCount(): Int
}
