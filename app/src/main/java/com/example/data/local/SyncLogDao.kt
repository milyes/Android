package com.example.data.local

import androidx.room.*
import com.example.data.model.SyncLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncLogDao {
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC")
    fun getAllSyncLogs(): Flow<List<SyncLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(syncLog: SyncLog): Long

    @Update
    suspend fun updateSyncLog(syncLog: SyncLog)

    @Query("DELETE FROM sync_logs")
    suspend fun clearLogs()
}
