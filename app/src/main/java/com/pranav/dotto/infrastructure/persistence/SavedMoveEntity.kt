package com.pranav.dotto.infrastructure.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_moves")
data class SavedMoveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val levelNumber: Int,
    val playerId: String,
    val lineType: String, // "Horizontal" or "Vertical"
    val row: Int,
    val column: Int,
    val timestamp: Long = System.currentTimeMillis()
)
