package com.gintama.nlcli.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gintama.nlcli.data.entity.TodoEntity

@Dao
interface TodoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoEntity): Long

    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, timestampMs DESC")
    suspend fun getAllTodos(): List<TodoEntity>

    @Query("UPDATE todos SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long): Int

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM todos WHERE isCompleted = 1")
    suspend fun clearCompleted(): Int

    @Query("DELETE FROM todos")
    suspend fun clearAll(): Int
}
