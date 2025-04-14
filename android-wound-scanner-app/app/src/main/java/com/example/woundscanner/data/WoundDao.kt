package com.example.woundscanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface WoundDao {
    // Basic CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWound(wound: WoundEntity): Long

    @Update
    suspend fun updateWound(wound: WoundEntity)

    @Delete
    suspend fun deleteWound(wound: WoundEntity)

    @Query("DELETE FROM wounds WHERE id = :woundId")
    suspend fun deleteWoundById(woundId: Long)

    // Basic queries
    @Query("SELECT * FROM wounds WHERE id = :woundId")
    fun getWoundById(woundId: Long): Flow<WoundEntity?>

    @Query("SELECT * FROM wounds ORDER BY date DESC")
    fun getAllWounds(): Flow<List<WoundEntity>>

    @Query("SELECT * FROM wounds WHERE isArchived = 0 ORDER BY date DESC")
    fun getActiveWounds(): Flow<List<WoundEntity>>

    // Recent wounds
    @Query("""
        SELECT * FROM wounds 
        WHERE date >= :startDate 
        ORDER BY date DESC 
        LIMIT :limit
    """)
    fun getRecentWounds(startDate: Date, limit: Int): Flow<List<WoundEntity>>

    // Search and filter
    @Query("""
        SELECT * FROM wounds 
        WHERE type LIKE '%' || :query || '%' 
        OR severity LIKE '%' || :query || '%'
        OR notes LIKE '%' || :query || '%'
        ORDER BY date DESC
    """)
    fun searchWounds(query: String): Flow<List<WoundEntity>>

    @Query("""
        SELECT * FROM wounds 
        WHERE type = :type 
        AND severity = :severity 
        AND date BETWEEN :startDate AND :endDate
        ORDER BY date DESC
    """)
    fun filterWounds(
        type: String,
        severity: String,
        startDate: Date,
        endDate: Date
    ): Flow<List<WoundEntity>>

    // Statistics
    @Query("SELECT COUNT(*) FROM wounds")
    fun getTotalWoundCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM wounds WHERE healingProgress >= 1.0")
    fun getHealedWoundCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM wounds WHERE isHighPriority = 1")
    fun getHighPriorityWoundCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM wounds 
        WHERE requiresFollowUp = 1 
        AND followUpDate < :currentDate
    """)
    fun getOverdueFollowUpsCount(currentDate: Date): Flow<Int>

    @Query("""
        SELECT AVG(
            CAST(
                (julianday(CASE 
                    WHEN healingProgress >= 1.0 THEN updatedAt 
                    ELSE date('now') 
                END) - julianday(date)) AS INTEGER
            )
        ) 
        FROM wounds 
        WHERE healingProgress >= 1.0
    """)
    fun getAverageHealingTime(): Flow<Float>

    // Type and severity distribution
    @Query("""
        SELECT type, COUNT(*) as count 
        FROM wounds 
        GROUP BY type 
        ORDER BY count DESC
    """)
    fun getWoundTypeDistribution(): Flow<Map<String, Int>>

    @Query("""
        SELECT severity, COUNT(*) as count 
        FROM wounds 
        GROUP BY severity 
        ORDER BY count DESC
    """)
    fun getWoundSeverityDistribution(): Flow<Map<String, Int>>

    // Follow-ups
    @Query("""
        SELECT * FROM wounds 
        WHERE requiresFollowUp = 1 
        AND followUpDate >= :currentDate 
        ORDER BY followUpDate ASC
    """)
    fun getUpcomingFollowUps(currentDate: Date): Flow<List<WoundEntity>>

    @Query("""
        SELECT * FROM wounds 
        WHERE requiresFollowUp = 1 
        AND followUpDate < :currentDate 
        ORDER BY followUpDate DESC
    """)
    fun getOverdueFollowUps(currentDate: Date): Flow<List<WoundEntity>>

    // Healing progress tracking
    @Query("""
        SELECT * FROM wounds 
        WHERE healingProgress < 1.0 
        ORDER BY date DESC
    """)
    fun getHealingInProgressWounds(): Flow<List<WoundEntity>>

    @Query("""
        SELECT * FROM wounds 
        WHERE healingProgress >= 1.0 
        ORDER BY updatedAt DESC
    """)
    fun getHealedWounds(): Flow<List<WoundEntity>>

    // Archive management
    @Query("UPDATE wounds SET isArchived = 1 WHERE id = :woundId")
    suspend fun archiveWound(woundId: Long)

    @Query("UPDATE wounds SET isArchived = 0 WHERE id = :woundId")
    suspend fun unarchiveWound(woundId: Long)

    @Query("DELETE FROM wounds WHERE isArchived = 1 AND date < :date")
    suspend fun deleteArchivedWoundsBefore(date: Date)

    // Batch operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWounds(wounds: List<WoundEntity>)

    @Update
    suspend fun updateWounds(wounds: List<WoundEntity>)

    @Delete
    suspend fun deleteWounds(wounds: List<WoundEntity>)

    @Query("DELETE FROM wounds")
    suspend fun deleteAllWounds()

    // Custom queries for analytics
    @Query("""
        SELECT 
            COUNT(*) as totalCount,
            SUM(CASE WHEN healingProgress >= 1.0 THEN 1 ELSE 0 END) as healedCount,
            SUM(CASE WHEN healingProgress < 1.0 THEN 1 ELSE 0 END) as activeCount,
            AVG(CASE 
                WHEN healingProgress >= 1.0 
                THEN (julianday(updatedAt) - julianday(date)) 
                ELSE NULL 
            END) as avgHealingTime
        FROM wounds
    """)
    fun getWoundStatistics(): Flow<WoundStatistics>

    // Trending analysis
    @Query("""
        SELECT type, COUNT(*) as count 
        FROM wounds 
        WHERE date >= :startDate 
        GROUP BY type 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    fun getTrendingWoundTypes(startDate: Date, limit: Int): Flow<Map<String, Int>>

    @Query("""
        SELECT severity, COUNT(*) as count 
        FROM wounds 
        WHERE date >= :startDate 
        GROUP BY severity 
        ORDER BY count DESC
    """)
    fun getTrendingSeverityLevels(startDate: Date): Flow<Map<String, Int>>
}
