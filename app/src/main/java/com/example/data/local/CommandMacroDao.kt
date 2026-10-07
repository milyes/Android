package com.example.data.local

import androidx.room.*
import com.example.data.model.CommandMacro
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandMacroDao {
    @Query("SELECT * FROM command_macros ORDER BY category ASC, id ASC")
    fun getAllMacros(): Flow<List<CommandMacro>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: CommandMacro): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(macros: List<CommandMacro>)

    @Query("DELETE FROM command_macros WHERE id = :id")
    suspend fun deleteMacro(id: Long)

    @Update
    suspend fun updateMacro(macro: CommandMacro)

    @Query("UPDATE command_macros SET isFavorite = :isFav WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFav: Boolean)

    @Query("SELECT COUNT(*) FROM command_macros")
    suspend fun getCount(): Int
}
