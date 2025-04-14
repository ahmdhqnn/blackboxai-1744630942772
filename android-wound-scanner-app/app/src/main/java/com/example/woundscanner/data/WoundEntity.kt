package com.example.woundscanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.*

@Entity(tableName = "wounds")
@TypeConverters(Converters::class)
data class WoundEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Image information
    val imageUri: String,
    
    // Wound classification
    val type: String,
    val size: String,
    val severity: String,
    val confidence: Float,
    
    // Treatment information
    val recommendations: String,
    val notes: String? = null,
    
    // Timestamps
    val date: Date,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val followUpDate: Date? = null,
    
    // Progress tracking
    val healingProgress: Float = 0f,
    val healingStage: String? = null,
    
    // Additional metadata
    val location: String? = null,
    val patientId: String? = null,
    val treatmentPlan: String? = null,
    val complications: String? = null,
    val measurementHistory: String? = null,
    
    // Tags and flags
    val tags: List<String> = emptyList(),
    val isArchived: Boolean = false,
    val requiresFollowUp: Boolean = false,
    val isHighPriority: Boolean = false
)

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        return value?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    @TypeConverter
    fun stringListToString(list: List<String>?): String {
        return list?.joinToString(",") ?: ""
    }
}

// Helper class for wound statistics
data class WoundStatistics(
    val totalCount: Int = 0,
    val healedCount: Int = 0,
    val activeCount: Int = 0,
    val averageHealingTime: Long = 0,
    val typeDistribution: Map<String, Int> = emptyMap(),
    val severityDistribution: Map<String, Int> = emptyMap()
)

// Helper class for wound measurements
data class WoundMeasurement(
    val date: Date,
    val length: Float,
    val width: Float,
    val depth: Float? = null,
    val area: Float = length * width,
    val volume: Float? = depth?.let { area * it },
    val notes: String? = null
)

// Helper class for wound healing stages
enum class HealingStage {
    INITIAL_ASSESSMENT,
    INFLAMMATION,
    PROLIFERATION,
    MATURATION,
    HEALED,
    COMPLICATED;

    companion object {
        fun fromString(value: String?): HealingStage? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}

// Helper class for wound severity levels
enum class SeverityLevel {
    MILD,
    MODERATE,
    SEVERE;

    companion object {
        fun fromString(value: String?): SeverityLevel? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}

// Helper class for wound types
enum class WoundType {
    ABRASION,
    LACERATION,
    BURN,
    PRESSURE_ULCER,
    SURGICAL;

    companion object {
        fun fromString(value: String?): WoundType? {
            return values().find { it.name.replace("_", " ").equals(value, ignoreCase = true) }
        }
    }
}

// Helper class for wound locations
data class WoundLocation(
    val bodyPart: String,
    val side: Side? = null,
    val position: Position? = null
) {
    enum class Side {
        LEFT, RIGHT, CENTRAL
    }

    enum class Position {
        ANTERIOR, POSTERIOR, LATERAL, MEDIAL
    }

    override fun toString(): String {
        return buildString {
            side?.let { append("$it ") }
            append(bodyPart)
            position?.let { append(" ($it)") }
        }
    }

    companion object {
        fun fromString(value: String?): WoundLocation? {
            if (value == null) return null

            val parts = value.split(" ", "(", ")")
            return WoundLocation(
                bodyPart = parts.getOrNull(1) ?: parts[0],
                side = parts.getOrNull(0)?.let { Side.valueOf(it.uppercase()) },
                position = parts.getOrNull(2)?.let { Position.valueOf(it.uppercase()) }
            )
        }
    }
}

// Helper class for treatment plans
data class TreatmentPlan(
    val treatments: List<Treatment>,
    val frequency: Frequency,
    val duration: Int, // in days
    val startDate: Date,
    val endDate: Date = Calendar.getInstance().apply {
        time = startDate
        add(Calendar.DAY_OF_YEAR, duration)
    }.time,
    val notes: String? = null
) {
    data class Treatment(
        val type: String,
        val description: String,
        val materials: List<String>
    )

    enum class Frequency {
        ONCE_DAILY,
        TWICE_DAILY,
        THREE_TIMES_DAILY,
        WEEKLY,
        AS_NEEDED
    }
}
