package com.gintama.nlcli.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gintama.nlcli.data.entity.MacroEntity

@Dao
interface MacroDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(macro: MacroEntity)

    @Query("SELECT * FROM macros WHERE name = :name LIMIT 1")
    suspend fun getMacro(name: String): MacroEntity?

    @Query("SELECT * FROM macros ORDER BY name ASC")
    suspend fun getAllMacros(): List<MacroEntity>

    @Query("DELETE FROM macros WHERE name = :name")
    suspend fun deleteByName(name: String): Int

    @Query("DELETE FROM macros")
    suspend fun clearAll(): Int
}
