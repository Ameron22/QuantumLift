package com.example.gymtracker.data
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        EntityExercise::class,
        EntityWorkout::class,
        CrossRefWorkoutExercise::class,
        SessionWorkoutEntity::class,
        SessionEntityExercise::class,
        AchievementEntity::class
    ],
    version = 22,  // Increment version number
    //exportSchema = false
)
@TypeConverters(Converter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration()  // This will recreate tables if schema changes
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}