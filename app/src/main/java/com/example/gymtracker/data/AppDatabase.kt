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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        EntityExercise::class,
        EntityWorkout::class,
        CrossRefWorkoutExercise::class,
        SessionWorkoutEntity::class,
        SessionEntityExercise::class,
        AchievementEntity::class
    ],
    version = 33,  // Increment version number to force database recreation
    exportSchema = false
)
@TypeConverters(Converter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun achievementDao(): AchievementDao

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