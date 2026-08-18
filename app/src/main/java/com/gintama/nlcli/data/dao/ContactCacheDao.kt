package com.gintama.nlcli.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gintama.nlcli.data.entity.ContactCacheEntity

@Dao
interface ContactCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: ContactCacheEntity)

    @Query("SELECT * FROM contact_cache WHERE lookupKey = :lookupKey LIMIT 1")
    suspend fun findByLookupKey(lookupKey: String): ContactCacheEntity?

    @Query("SELECT * FROM contact_cache ORDER BY lastUsedTimestampMs DESC LIMIT :limit")
    suspend fun getRecentContacts(limit: Int = 10): List<ContactCacheEntity>

    @Query("DELETE FROM contact_cache WHERE lookupKey = :lookupKey")
    suspend fun delete(lookupKey: String)

    @Query("DELETE FROM contact_cache")
    suspend fun clearCache()
}
