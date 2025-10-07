package com.example.gymtracker.data

import androidx.room.*
import com.example.gymtracker.classes.SessionWorkoutWithMusclesStress
import com.example.gymtracker.data.Converter
import kotlinx.coroutines.flow.Flow

// Data class to combine exercise info with workout-specific data
data class ExerciseWithWorkoutData(
    val exercise: EntityExercise,
    val workoutExercise: WorkoutExercise
)

// Data class to combine exercise alternative with exercise details
data class ExerciseAlternativeWithDetails(
    val alternative: ExerciseAlternative,
    val exercise: EntityExercise
)

@Dao
interface ExerciseDao {
    // Add this method to check if the database is empty
    @Query("SELECT COUNT(*) FROM EntityWorkout")
    suspend fun getWorkoutCount(): Int

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: EntityExercise): Long

    @Update
    suspend fun updateExercise(exercise: EntityExercise)

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercises(): List<EntityExercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: EntityWorkout): Long

    @Update
    suspend fun updateWorkout(workout: EntityWorkout)

    @Delete
    suspend fun deleteWorkout(workout: EntityWorkout)

    @Delete
    suspend fun deleteExercise(exercise: EntityExercise)

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getExerciseById(exerciseId: Int): EntityExercise?

    @Transaction
    @Query("SELECT * FROM EntityWorkout WHERE id = :workoutId")
    suspend fun getWorkoutWithExercises(workoutId: Int): List<WorkoutWithExercises>

    @Query("SELECT * FROM EntityWorkout")
    suspend fun getAllWorkouts(): List<EntityWorkout>

    @Insert
    suspend fun insertWorkoutSession(entity: SessionWorkoutEntity): Long

    @Update
    suspend fun updateWorkoutSession(entity: SessionWorkoutEntity)

    @Insert
    suspend fun insertExerciseSession(entity: SessionEntityExercise)

    @Update
    suspend fun updateExerciseSession(entity: SessionEntityExercise)

    @Query("SELECT * FROM exercise_sessions")
    fun getAllExerciseSessions(): Flow<List<SessionEntityExercise>>

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllWorkoutSessionsFlow(): Flow<List<SessionWorkoutEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    suspend fun getAllWorkoutSessions(): List<SessionWorkoutEntity>

    @Query("UPDATE workout_sessions Set endTime = :endTime WHERE sessionId = :sessionId")
    suspend fun updateWorkoutSessionEndTime(sessionId: Long, endTime: Long)

    @Query(
        "SELECT ws.sessionId, ws.workoutId, ws.startTime, ws.endTime, ws.workoutName, es.muscleGroup, es.sets, es.repsOrTime, es.weight " +
                "FROM workout_sessions ws INNER JOIN exercise_sessions es " +
                "ON ws.sessionId = es.sessionId " +
                "GROUP BY ws.sessionId, es.muscleGroup " +
                "ORDER BY ws.startTime DESC"
    )
    fun getAllWorkoutSessionsWithMuscleStress(): Flow<List<SessionWorkoutWithMusclesStress>>

    @Query("SELECT * FROM workout_sessions WHERE workoutId = :workoutId ORDER BY startTime DESC")
    suspend fun getWorkoutSessionsByWorkoutId(workoutId: Int): List<SessionWorkoutEntity>

    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun getWorkoutSession(sessionId: Long): SessionWorkoutEntity?

    @Query("SELECT COUNT(DISTINCT sessionId) FROM workout_sessions")
    suspend fun getTotalWorkoutCount(): Int

    @Query("SELECT startTime FROM workout_sessions ORDER BY startTime DESC")
    suspend fun getWorkoutDates(): List<Long>

    @Query("SELECT MAX(weight) FROM exercise_sessions WHERE exerciseId IN (SELECT id FROM exercises WHERE name = :exerciseName)")
    suspend fun getMaxWeightForExercise(exerciseName: String): Float

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    suspend fun getAllWorkoutSessionsOrderedByDate(): List<SessionWorkoutEntity>

    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionWorkoutById(sessionId: Long): SessionWorkoutEntity?

    // --- WorkoutExercise methods ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExercise): Long

    @Update
    suspend fun updateWorkoutExercise(workoutExercise: WorkoutExercise)

    @Delete
    suspend fun deleteWorkoutExercise(workoutExercise: WorkoutExercise)

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY `order` ASC")
    suspend fun getWorkoutExercisesForWorkout(workoutId: Int): List<WorkoutExercise>

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    suspend fun getWorkoutExerciseById(id: Int): WorkoutExercise?

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId AND exerciseId = :exerciseId")
    suspend fun getWorkoutExercise(workoutId: Int, exerciseId: Int): WorkoutExercise?

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteWorkoutExercisesForWorkout(workoutId: Int)

    @Query("SELECT MAX(`order`) FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun getMaxExerciseOrder(workoutId: Int): Int?

    @Query("UPDATE workout_exercises SET `order` = :newOrder WHERE id = :workoutExerciseId")
    suspend fun updateWorkoutExerciseOrder(workoutExerciseId: Int, newOrder: Int)

    // New method to get exercises with their workout-specific data
    @Transaction
    suspend fun getExercisesWithWorkoutData(workoutId: Int): List<ExerciseWithWorkoutData> {
        val workoutExercises = getWorkoutExercisesForWorkout(workoutId)
        val exercises = getAllExercises()

        return workoutExercises.mapNotNull { we ->
            val exercise = exercises.find { it.id == we.exerciseId }
            exercise?.let { ExerciseWithWorkoutData(it, we) }
        }
    }

    @Query("DELETE FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun deleteWorkoutSessionById(sessionId: Long)

    @Query("DELETE FROM exercise_sessions WHERE sessionId = :sessionId")
    suspend fun deleteExerciseSessionsBySessionId(sessionId: Long)

    @Query("SELECT * FROM exercise_sessions WHERE sessionId = :sessionId")
    suspend fun getExerciseSessionsForSession(sessionId: Long): List<SessionEntityExercise>

    // Get the latest exercise session for a specific exercise
    @Query("SELECT * FROM exercise_sessions WHERE exerciseId = :exerciseId ORDER BY exerciseSessionId DESC LIMIT 1")
    suspend fun getLatestExerciseSession(exerciseId: Long): SessionEntityExercise?

    // Get the latest exercise session with soreness factors for a specific exercise
    @Query("SELECT * FROM exercise_sessions WHERE exerciseId = :exerciseId AND eccentricFactor != 1.0 AND noveltyFactor != 5 AND adaptationLevel != 5 AND rpe != 5 AND subjectiveSoreness != 5 ORDER BY exerciseSessionId DESC LIMIT 1")
    suspend fun getLatestExerciseSessionWithSorenessFactors(exerciseId: Long): SessionEntityExercise?

    // --- Exercise Alternative methods ---
    @Insert
    suspend fun insertExerciseAlternative(alternative: ExerciseAlternative): Long

    @Update
    suspend fun updateExerciseAlternative(alternative: ExerciseAlternative)

    @Delete
    suspend fun deleteExerciseAlternative(alternative: ExerciseAlternative)

    @Query("SELECT * FROM exercise_alternatives WHERE workoutExerciseId = :workoutExerciseId ORDER BY `order` ASC")
    suspend fun getExerciseAlternatives(workoutExerciseId: Int): List<ExerciseAlternative>

    @Query("UPDATE exercise_alternatives SET isActive = 0 WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun deactivateAllAlternatives(workoutExerciseId: Int)

    @Query("UPDATE exercise_alternatives SET isActive = 1 WHERE id = :alternativeId")
    suspend fun activateAlternative(alternativeId: Int)

    @Query("DELETE FROM exercise_alternatives WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun deleteAllAlternativesForWorkoutExercise(workoutExerciseId: Int)

    @Query("DELETE FROM exercise_alternatives WHERE workoutExerciseId IN (SELECT id FROM workout_exercises WHERE workoutId = :workoutId)")
    suspend fun deleteAllAlternativesForWorkout(workoutId: Int)

    @Query("UPDATE workout_exercises SET exerciseId = :newExerciseId WHERE id = :workoutExerciseId")
    suspend fun updateWorkoutExerciseId(workoutExerciseId: Int, newExerciseId: Int)

    @Query("UPDATE workout_exercises SET hasAlternatives = :hasAlternatives WHERE id = :workoutExerciseId")
    suspend fun updateWorkoutExerciseHasAlternatives(
        workoutExerciseId: Int,
        hasAlternatives: Boolean
    )

    // Get similar exercises based on muscle group and overlapping muscle parts
    // Uses LIKE operator to check for overlapping muscle parts (compatible with older SQLite)
    @Query("""
        SELECT * FROM exercises 
        WHERE muscle = :muscleGroup 
        AND id != :excludeId 
        AND (
            parts = :muscleParts 
            OR parts LIKE '%' || :musclePart1 || '%'
            OR parts LIKE '%' || :musclePart2 || '%'
            OR parts LIKE '%' || :musclePart3 || '%'
            OR parts LIKE '%' || :musclePart4 || '%'
            OR parts LIKE '%' || :musclePart5 || '%'
        )
        LIMIT :limit
    """)
    suspend fun getSimilarExercises(
        muscleGroup: String,
        muscleParts: String,
        musclePart1: String,
        musclePart2: String,
        musclePart3: String,
        musclePart4: String,
        musclePart5: String,
        excludeId: Int,
        limit: Int = 10
    ): List<EntityExercise>

    // Get exercises by muscle group only (for broader alternatives)
    @Query("SELECT * FROM exercises WHERE muscle = :muscleGroup AND id != :excludeId LIMIT :limit")
    suspend fun getExercisesByMuscleGroup(
        muscleGroup: String,
        excludeId: Int,
        limit: Int = 10
    ): List<EntityExercise>

    // Get filtered similar exercises with multiple criteria
    // Uses muscle group + overlapping muscle parts + equipment/difficulty filters
    @Query("""
        SELECT * FROM exercises 
        WHERE muscle = :muscleGroup 
        AND id != :excludeId 
        AND (
            parts = :muscleParts 
            OR parts LIKE '%' || :musclePart1 || '%'
            OR parts LIKE '%' || :musclePart2 || '%'
            OR parts LIKE '%' || :musclePart3 || '%'
            OR parts LIKE '%' || :musclePart4 || '%'
            OR parts LIKE '%' || :musclePart5 || '%'
        )
        AND (:equipment = '' OR equipment LIKE '%' || :equipment || '%' OR (:equipment = 'None' AND (equipment = '' OR equipment IS NULL)))
        AND (:difficulty = '' OR difficulty = :difficulty)
        LIMIT :limit
    """)
    suspend fun getFilteredSimilarExercises(
        muscleGroup: String,
        muscleParts: String,
        musclePart1: String,
        musclePart2: String,
        musclePart3: String,
        musclePart4: String,
        musclePart5: String,
        equipment: String,
        difficulty: String,
        excludeId: Int,
        limit: Int = 20
    ): List<EntityExercise>

    // Get exercise alternatives with their exercise details
    @Transaction
    suspend fun getExerciseAlternativesWithDetails(workoutExerciseId: Int): List<ExerciseAlternativeWithDetails> {
        val alternatives = getExerciseAlternatives(workoutExerciseId)
        val exercises = getAllExercises()

        return alternatives.mapNotNull { alternative ->
            val exercise = exercises.find { it.id == alternative.alternativeExerciseId }
            exercise?.let { ExerciseAlternativeWithDetails(alternative, it) }
        }
    }


    // Helper function to get filtered similar exercises with parsed muscle parts
    suspend fun getFilteredSimilarExercisesWithParsedParts(
        muscleGroup: String,
        muscleParts: String,
        equipment: String,
        difficulty: String,
        excludeId: Int,
        limit: Int = 20
    ): List<EntityExercise> {
        val converter = Converter()
        val partsList = converter.fromString(muscleParts)
        
        // Safely pad the list to exactly 5 elements with empty strings
        // Take first 5 elements if more than 5, pad with empty strings if less than 5
        val paddedParts = (partsList.take(5) + List(5.coerceAtLeast(0)) { "" }).take(5)
        
        val allExercises = getFilteredSimilarExercises(
            muscleGroup = muscleGroup,
            muscleParts = muscleParts,
            musclePart1 = paddedParts[0],
            musclePart2 = paddedParts[1],
            musclePart3 = paddedParts[2],
            musclePart4 = paddedParts[3],
            musclePart5 = paddedParts[4],
            equipment = equipment,
            difficulty = difficulty,
            excludeId = excludeId,
            limit = limit * 3 // Get more results to ensure we can meet the limit after filtering
        )
        
        // Apply additional filtering to exclude exercises with conflicting muscle part names
        return filterExercisesByMusclePartNames(allExercises, partsList, limit)
    }

    // Helper function to get similar exercises with parsed muscle parts (no equipment/difficulty filter)
    suspend fun getSimilarExercisesWithParsedParts(
        muscleGroup: String,
        muscleParts: String,
        excludeId: Int,
        limit: Int = 10
    ): List<EntityExercise> {
        val converter = Converter()
        val partsList = converter.fromString(muscleParts)
        
        // Safely pad the list to exactly 5 elements with empty strings
        // Take first 5 elements if more than 5, pad with empty strings if less than 5
        val paddedParts = (partsList.take(5) + List(5.coerceAtLeast(0)) { "" }).take(5)
        
        val allExercises = getSimilarExercises(
            muscleGroup = muscleGroup,
            muscleParts = muscleParts,
            musclePart1 = paddedParts[0],
            musclePart2 = paddedParts[1],
            musclePart3 = paddedParts[2],
            musclePart4 = paddedParts[3],
            musclePart5 = paddedParts[4],
            excludeId = excludeId,
            limit = limit * 3 // Get more results to ensure we can meet the limit after filtering
        )
        
        // Apply additional filtering to exclude exercises with conflicting muscle part names
        return filterExercisesByMusclePartNames(allExercises, partsList, limit)
    }

    // Helper function to filter exercises by muscle part names in their titles
    private fun filterExercisesByMusclePartNames(
        exercises: List<EntityExercise>,
        currentMuscleParts: List<String>,
        limit: Int
    ): List<EntityExercise> {
        // Create a map of muscle part names to their common variations
        val musclePartVariations = mapOf(
            "Biceps" to listOf("bicep", "biceps", "curl"),
            "Triceps" to listOf("tricep", "triceps", "extension", "dip"),
            "Chest" to listOf("chest", "pec", "bench", "press", "fly", "flye"),
            "Back" to listOf("back", "lat", "row", "pull", "pulldown"),
            "Shoulders" to listOf("shoulder", "deltoid", "lateral", "rear", "military", "press"),
            "Quadriceps" to listOf("quad", "quads", "squat", "leg press", "extension"),
            "Hamstrings" to listOf("hamstring", "hams", "curl", "deadlift"),
            "Glutes" to listOf("glute", "glutes", "hip", "thrust", "bridge"),
            "Calves" to listOf("calf", "calves", "raise"),
            "Abs" to listOf("ab", "abs", "crunch", "situp", "plank"),
            "Obliques" to listOf("oblique", "twist", "side"),
            "Forearms" to listOf("forearm", "grip", "wrist"),
            "Lats" to listOf("lat", "lats", "pull", "pulldown"),
            "Upper Back" to listOf("upper back", "rhomboid", "trap", "shrug"),
            "Lower Back" to listOf("lower back", "erector", "hyperextension"),
            "Front Deltoids" to listOf("front delt", "front deltoid", "military", "press"),
            "Middle Deltoids" to listOf("middle delt", "middle deltoid", "lateral", "raise"),
            "Rear Deltoids" to listOf("rear delt", "rear deltoid", "reverse", "fly")
        )
        
        // Get all muscle part variations for the current exercise
        val currentMusclePartVariations = currentMuscleParts.flatMap { part ->
            musclePartVariations[part] ?: listOf(part.lowercase())
        }.toSet()
        
        // Get all other muscle part variations (not in current exercise)
        val otherMusclePartVariations = musclePartVariations.entries
            .filter { entry -> !currentMuscleParts.contains(entry.key) }
            .flatMap { it.value }
            .toSet()
        
        return exercises.filter { exercise ->
            val exerciseNameLower = exercise.name.lowercase()
            
            // Check if exercise name contains muscle part variations that are NOT in current exercise
            val containsConflictingMuscleParts = otherMusclePartVariations.any { variation ->
                exerciseNameLower.contains(variation)
            }
            
            // Check if exercise name contains muscle part variations that ARE in current exercise
            val containsMatchingMuscleParts = currentMusclePartVariations.any { variation ->
                exerciseNameLower.contains(variation)
            }
            
            // Keep exercises that either:
            // 1. Don't contain any conflicting muscle part names, OR
            // 2. Contain matching muscle part names (even if they also contain conflicting ones)
            !containsConflictingMuscleParts || containsMatchingMuscleParts
        }.take(limit)
    }
}