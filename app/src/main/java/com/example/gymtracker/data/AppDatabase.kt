package com.example.gymtracker.data
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        EntityExercise::class,
        EntityWorkout::class,
        CrossRefWorkoutExercise::class,
        SessionEntityExercise::class,
        SessionWorkoutEntity::class
    ],
    version = 18,  // Increment version number
    exportSchema = false
)
@TypeConverters(Converter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
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