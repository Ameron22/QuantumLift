package com.example.gymtracker.data
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [EntityExercise::class,
        EntityWorkout::class,
        CrossRefWorkoutExercise::class,
        SessionWorkoutEntity::class, // Add this
        SessionEntityExercise::class // Add this
    ],
    version = 12, // Increase version number when schema changes ( you need to change version when you make changes to DB)
    exportSchema = false // Disable schema export
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
                    .fallbackToDestructiveMigration() // THIS LINE IS THE SOLUTION (this line destroys old databases, used when version changes)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}