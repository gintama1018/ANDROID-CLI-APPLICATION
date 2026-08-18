package com.gintama.nlcli.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gintama.nlcli.data.dao.CommandHistoryDao
import com.gintama.nlcli.data.dao.ContactCacheDao
import com.gintama.nlcli.data.dao.MacroDao
import com.gintama.nlcli.data.dao.NoteDao
import com.gintama.nlcli.data.dao.SnippetDao
import com.gintama.nlcli.data.dao.TodoDao
import com.gintama.nlcli.data.entity.CommandHistoryEntity
import com.gintama.nlcli.data.entity.ContactCacheEntity
import com.gintama.nlcli.data.entity.MacroEntity
import com.gintama.nlcli.data.entity.NoteEntity
import com.gintama.nlcli.data.entity.SnippetEntity
import com.gintama.nlcli.data.entity.TodoEntity

@Database(
    entities = [
        CommandHistoryEntity::class,
        ContactCacheEntity::class,
        NoteEntity::class,
        TodoEntity::class,
        SnippetEntity::class,
        MacroEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun contactCacheDao(): ContactCacheDao
    abstract fun noteDao(): NoteDao
    abstract fun todoDao(): TodoDao
    abstract fun snippetDao(): SnippetDao
    abstract fun macroDao(): MacroDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nlcli_database.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
