package com.example.woundscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

@Database(
    entities = [WoundEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WoundDatabase : RoomDatabase() {
    abstract fun woundDao(): WoundDao

    companion object {
        @Volatile
        private var INSTANCE: WoundDatabase? = null

        fun getDatabase(context: Context): WoundDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WoundDatabase::class.java,
                    "wound_database"
                )
                    .addCallback(WoundDatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class WoundDatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(database.woundDao())
                }
            }
        }

        private suspend fun populateDatabase(woundDao: WoundDao) {
            // Delete all content
            woundDao.deleteAllWounds()

            // Add sample data
            val sampleWounds = listOf(
                WoundEntity(
                    imageUri = "content://sample/abrasion.jpg",
                    type = "Abrasion",
                    size = "2.5 x 3.0 cm",
                    severity = "Mild",
                    confidence = 0.92f,
                    recommendations = """
                        1. Clean the wound with mild soap and water
                        2. Apply antibiotic ointment
                        3. Cover with sterile dressing
                        4. Change dressing daily
                        5. Monitor for signs of infection
                    """.trimIndent(),
                    notes = "Minor abrasion on right knee from fall",
                    date = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -5)
                    }.time,
                    healingProgress = 0.7f,
                    healingStage = "Proliferation",
                    location = "Right Knee",
                    requiresFollowUp = false
                ),
                WoundEntity(
                    imageUri = "content://sample/laceration.jpg",
                    type = "Laceration",
                    size = "4.0 x 1.5 cm",
                    severity = "Moderate",
                    confidence = 0.88f,
                    recommendations = """
                        1. Seek medical evaluation for potential stitches
                        2. Keep wound elevated if possible
                        3. Apply prescribed treatment
                        4. Change dressing as directed
                        5. Schedule follow-up appointment
                    """.trimIndent(),
                    notes = "Deep cut on left forearm requiring stitches",
                    date = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -10)
                    }.time,
                    healingProgress = 0.4f,
                    healingStage = "Inflammation",
                    location = "Left Forearm",
                    requiresFollowUp = true,
                    followUpDate = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, 4)
                    }.time,
                    isHighPriority = true
                ),
                WoundEntity(
                    imageUri = "content://sample/burn.jpg",
                    type = "Burn",
                    size = "5.0 x 5.0 cm",
                    severity = "Severe",
                    confidence = 0.95f,
                    recommendations = """
                        1. Seek immediate emergency care
                        2. Do not apply ointments
                        3. Cover loosely with clean, dry dressing
                        4. Follow medical professional's instructions
                        5. Specialized burn care may be required
                    """.trimIndent(),
                    notes = "Third-degree burn on right hand",
                    date = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -15)
                    }.time,
                    healingProgress = 0.2f,
                    healingStage = "Initial Assessment",
                    location = "Right Hand",
                    requiresFollowUp = true,
                    followUpDate = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, 2)
                    }.time,
                    isHighPriority = true,
                    treatmentPlan = "Daily specialized burn treatment at clinic"
                )
            )

            woundDao.insertWounds(sampleWounds)
        }
    }
}

// Database migrations
object DatabaseMigrations {
    // Add migrations here when needed
    // Example:
    /*
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                ALTER TABLE wounds 
                ADD COLUMN treatmentPlan TEXT
            """)
        }
    }
    */
}

// Database constants
object DatabaseConstants {
    const val DATABASE_NAME = "wound_database"
    const val DATABASE_VERSION = 1
    
    // Table names
    const val TABLE_WOUNDS = "wounds"
    
    // Column names (if needed outside of Room)
    const val COLUMN_ID = "id"
    const val COLUMN_TYPE = "type"
    const val COLUMN_SEVERITY = "severity"
    const val COLUMN_DATE = "date"
    
    // Query constants
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_SEARCH_RESULTS = 50
    const val RECENT_ITEMS_LIMIT = 10
}
