package com.pranav.dotto.infrastructure.persistence

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM player_progress WHERE id = 0")
    fun getProgress(): Flow<PlayerProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: PlayerProgressEntity)

    @Query("SELECT * FROM saved_moves ORDER BY timestamp ASC")
    suspend fun getAllMoves(): List<SavedMoveEntity>

    @Insert
    suspend fun insertMove(move: SavedMoveEntity)

    @Query("DELETE FROM saved_moves")
    suspend fun clearAllMoves()
}
