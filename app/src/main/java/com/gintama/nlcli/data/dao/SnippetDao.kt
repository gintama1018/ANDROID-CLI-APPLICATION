package com.gintama.nlcli.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gintama.nlcli.data.entity.SnippetEntity

@Dao
interface SnippetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snippet: SnippetEntity)

    @Query("SELECT * FROM snippets WHERE name = :name LIMIT 1")
    suspend fun getSnippet(name: String): SnippetEntity?

    @Query("SELECT * FROM snippets ORDER BY name ASC")
    suspend fun getAllSnippets(): List<SnippetEntity>

    @Query("DELETE FROM snippets WHERE name = :name")
    suspend fun deleteByName(name: String): Int

    @Query("DELETE FROM snippets")
    suspend fun clearAll(): Int
}
