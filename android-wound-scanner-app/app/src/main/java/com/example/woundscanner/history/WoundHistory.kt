package com.example.woundscanner.history

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "wound_history")
data class WoundHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val size: String,
    val severity: String,
    val date: Date,
    val notes: String? = null,
    val healingProgress: Float = 0f,
    val requiresFollowUp: Boolean = false,
    val followUpDate: Date? = null
) {
    // Additional methods or properties can be added here if needed
}
