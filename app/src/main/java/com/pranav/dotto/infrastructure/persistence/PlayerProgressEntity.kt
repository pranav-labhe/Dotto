package com.pranav.dotto.infrastructure.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_progress")
data class PlayerProgressEntity(
    @PrimaryKey val id: Int = 0, // Single row for now
    val playerName: String,
    val currentLevel: Int,
    val totalScore: Int,
    val highestLevelReached: Int,
    val nextPlayerId: String // "human_player" or "dotto_ai"
)
