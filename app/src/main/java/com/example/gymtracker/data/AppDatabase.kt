package com.example.gymtracker.data
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gymtracker.utils.ExerciseDataImporter
import com.example.gymtracker.data.Converter
import com.example.gymtracker.classes.InsertInitialData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Migration from version 45 to 46 - Add soreness assessment tables
val MIGRATION_45_46 = object : Migration(45, 46) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create soreness_assessments table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `soreness_assessments` (
                `assessmentId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sessionId` INTEGER NOT NULL,
                `exerciseId` INTEGER NOT NULL,
                `muscleGroups` TEXT NOT NULL,
                `muscleParts` TEXT NOT NULL,
                `soreness24hr` TEXT NOT NULL,
                `soreness48hr` TEXT,
                `overallSoreness` INTEGER NOT NULL,
                `assessmentDay` INTEGER NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `notes` TEXT,
                `wasActive` INTEGER NOT NULL,
                `sleepQuality` INTEGER,
                `stressLevel` INTEGER
            )
        """)
        
        // Create workout_context table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `workout_context` (
                `contextId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sessionId` INTEGER NOT NULL,
                `exerciseId` INTEGER NOT NULL,
                `muscleGroups` TEXT NOT NULL,
                `totalSets` INTEGER NOT NULL,
                `totalReps` INTEGER NOT NULL,
                `totalVolume` REAL NOT NULL,
                `avgWeight` REAL NOT NULL,
                `maxWeight` REAL NOT NULL,
                `weightProgression` REAL NOT NULL,
                `eccentricFactor` REAL NOT NULL,
                `noveltyFactor` INTEGER NOT NULL,
                `adaptationLevel` INTEGER NOT NULL,
                `rpe` INTEGER NOT NULL,
                `subjectiveSoreness` INTEGER NOT NULL,
                `daysSinceLastWorkout` INTEGER NOT NULL,
                `daysSinceLastMuscleGroup` TEXT NOT NULL,
                `totalWorkoutsThisWeek` INTEGER NOT NULL,
                `workoutDuration` INTEGER NOT NULL,
                `restBetweenSets` INTEGER NOT NULL,
                `recoveryFactors` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL
            )
        """)
        
        Log.d("AppDatabase", "Migration 45->46 completed: Added soreness assessment tables")
    }
}

// Migration from version 46 to 47 - Remove workout_context table temporarily
val MIGRATION_46_47 = object : Migration(46, 47) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Drop workout_context table temporarily
        database.execSQL("DROP TABLE IF EXISTS `workout_context`")
        
        Log.d("AppDatabase", "Migration 46->47 completed: Removed workout_context table temporarily")
    }
}

// Migration from version 47 to 48 - Add exercise alternatives functionality
val MIGRATION_47_48 = object : Migration(47, 48) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Check if hasAlternatives column already exists
                    val cursor = database.query("PRAGMA table_info(workout_exercises)")
                    var hasAlternativesExists = false
                    while (cursor.moveToNext()) {
                        val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                        if (columnName == "hasAlternatives") {
                            hasAlternativesExists = true
                            break
                        }
                    }
                    cursor.close()
                    
                    if (!hasAlternativesExists) {
                        // Add hasAlternatives column to workout_exercises table
                        database.execSQL("""
                            ALTER TABLE `workout_exercises` 
                            ADD COLUMN `hasAlternatives` INTEGER NOT NULL DEFAULT 0
                        """)
                    }
                    
                    // Create a new table with the correct schema and column order that Room expects
                    database.execSQL("""
                        CREATE TABLE `workout_exercises_new` (
                            `reps` INTEGER NOT NULL,
                            `hasAlternatives` INTEGER NOT NULL,
                            `exerciseId` INTEGER NOT NULL,
                            `sets` INTEGER NOT NULL,
                            `weight` INTEGER NOT NULL,
                            `workoutId` INTEGER NOT NULL,
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `order` INTEGER NOT NULL
                        )
                    """)
                    
                    // Copy data from old table to new table in the correct column order
                    database.execSQL("""
                        INSERT INTO `workout_exercises_new` 
                        SELECT `reps`, COALESCE(`hasAlternatives`, 0) as `hasAlternatives`, 
                               `exerciseId`, `sets`, `weight`, `workoutId`, `id`, `order`
                        FROM `workout_exercises`
                    """)
                    
                    // Drop old table and rename new table
                    database.execSQL("DROP TABLE `workout_exercises`")
                    database.execSQL("ALTER TABLE `workout_exercises_new` RENAME TO `workout_exercises`")
                    
                    // Create exercise_alternatives table with correct column order and no default values
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS `exercise_alternatives` (
                            `workoutExerciseId` INTEGER NOT NULL,
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `isActive` INTEGER NOT NULL,
                            `originalExerciseId` INTEGER NOT NULL,
                            `alternativeExerciseId` INTEGER NOT NULL,
                            `order` INTEGER NOT NULL
                        )
                    """)
                    
                    Log.d("AppDatabase", "Migration 47->48 completed: Added exercise alternatives functionality")
                    
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Error in migration 47->48", e)
                    throw e
                }
    }
}

// Migration from version 48 to 49 - Add database indexes for performance
val MIGRATION_48_49 = object : Migration(48, 49) {
    override fun migrate(database: SupportSQLiteDatabase) {
        try {
            // Add indexes to workout_exercises table
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_exercises_exerciseId` ON `workout_exercises` (`exerciseId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_exercises_workoutId` ON `workout_exercises` (`workoutId`)")
            
            // Add indexes to exercise_sessions table
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_sessions_sessionId` ON `exercise_sessions` (`sessionId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_sessions_exerciseId` ON `exercise_sessions` (`exerciseId`)")
            
            // Add indexes to workout_sessions table
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_workoutId` ON `workout_sessions` (`workoutId`)")
            
            // Add indexes to exercise_alternatives table
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_alternatives_originalExerciseId` ON `exercise_alternatives` (`originalExerciseId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_alternatives_alternativeExerciseId` ON `exercise_alternatives` (`alternativeExerciseId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_alternatives_workoutExerciseId` ON `exercise_alternatives` (`workoutExerciseId`)")
            
            Log.d("AppDatabase", "Migration 48->49 completed: Added database indexes for performance")
            
        } catch (e: Exception) {
            Log.e("AppDatabase", "Error in migration 48->49", e)
            throw e
        }
    }
}

@Database(
    entities = [
        EntityExercise::class,
        EntityWorkout::class,
        SessionWorkoutEntity::class,
        SessionEntityExercise::class,
        AchievementEntity::class,
        WorkoutExercise::class,
        PhysicalParameters::class,
        BodyMeasurement::class,
        UserXP::class,
        XPHistory::class,
        WarmUpTemplate::class,
        WarmUpExercise::class,
        WorkoutWarmUp::class,
        SorenessAssessment::class,
        ExerciseAlternative::class
        // WorkoutContext::class // Temporarily removed for debugging
    ],
    version = 49,  // Increment version number to add database indexes
    exportSchema = false
)
@TypeConverters(com.example.gymtracker.data.Converter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun achievementDao(): AchievementDao
    abstract fun physicalParametersDao(): PhysicalParametersDao
    abstract fun userXPDao(): UserXPDao
    abstract fun warmUpDao(): WarmUpDao
    abstract fun sorenessDao(): SorenessDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private var isInitialized = false

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.d("AppDatabase", "Creating new database instance")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(MIGRATION_45_46, MIGRATION_46_47, MIGRATION_47_48, MIGRATION_48_49)  // Add migrations to preserve existing data
                .fallbackToDestructiveMigration()  // Fallback for other version changes
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        Log.d("AppDatabase", "Database onCreate callback triggered")
                        if (!isInitialized) {
                            isInitialized = true
                            // Only import exercises from CSV when database is created
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    Log.d("AppDatabase", "Starting exercise import in onCreate")
                                    // Get the database instance that was just created
                                    val database = INSTANCE
                                                                if (database != null) {
                                val importer = ExerciseDataImporter(context.applicationContext, database.exerciseDao())
                                importer.importExercises()
                                Log.d("AppDatabase", "Exercise import completed")
                                
                                // Initialize warm-up templates
                                val insertInitialData = InsertInitialData()
                                insertInitialData.insertWarmUpTemplates(database.warmUpDao())
                                Log.d("AppDatabase", "Warm-up templates initialization completed")
                            } else {
                                Log.e("AppDatabase", "Database instance is null during onCreate")
                            }
                                } catch (e: Exception) {
                                    Log.e("AppDatabase", "Error during exercise import", e)
                                }
                            }
                        } else {
                            Log.d("AppDatabase", "Database already initialized, skipping exercise import")
                        }
                    }
                })
                .build()
                Log.d("AppDatabase", "Database instance created")
                INSTANCE = instance
                instance
            }
        }
    }
}