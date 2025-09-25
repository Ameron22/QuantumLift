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
        WorkoutWarmUp::class
    ],
    version = 45,  // Increment version number to add weight field to WarmUpExercise
    exportSchema = false
)
@TypeConverters(com.example.gymtracker.data.Converter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun achievementDao(): AchievementDao
    abstract fun physicalParametersDao(): PhysicalParametersDao
    abstract fun userXPDao(): UserXPDao
    abstract fun warmUpDao(): WarmUpDao

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
                .fallbackToDestructiveMigration()  // This will recreate tables if schema changes
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