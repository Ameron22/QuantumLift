package com.example.gymtracker.classes

import com.example.gymtracker.data.ExerciseDao
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.EntityWorkout
import com.example.gymtracker.data.CrossRefWorkoutExercise
import com.example.gymtracker.data.SessionWorkoutEntity
import com.example.gymtracker.data.SessionEntityExercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InsertInitialData {
    suspend fun insertInitialData(dao: ExerciseDao) = withContext(Dispatchers.IO) {
        // Check if the database is empty
        if (dao.getWorkoutCount() == 0) {
            try {
            // Insert Workouts
                val pushWorkoutId = dao.insertWorkout(EntityWorkout(name = "Push Day"))
                val pullWorkoutId = dao.insertWorkout(EntityWorkout(name = "Pull Day"))
                val legsWorkoutId = dao.insertWorkout(EntityWorkout(name = "Leg Day"))
                val quickUpperId = dao.insertWorkout(EntityWorkout(name = "Quick Upper Body"))
                val coreCardioId = dao.insertWorkout(EntityWorkout(name = "Core & Cardio"))

                // Insert Exercises with realistic data
                // Push exercises
                val benchPressId = dao.insertExercise(
                    EntityExercise(
                        name = "Bench Press",
                        sets = 4,
                        reps = 8,
                        weight = 60,
                        muscle = "Chest",
                        part = listOf("Upper Chest", "Middle Chest", "Front Deltoids")
                    )
                )
                val shoulderPressId = dao.insertExercise(
                    EntityExercise(
                        name = "Shoulder Press",
                        sets = 3,
                        reps = 10,
                        weight = 40,
                        muscle = "Shoulders",
                        part = listOf("Front Deltoids", "Middle Deltoids")
                    )
                )
                val tricepExtensionId = dao.insertExercise(
                    EntityExercise(
                        name = "Tricep Extension",
                        sets = 3,
                        reps = 12,
                        weight = 25,
                        muscle = "Arms",
                        part = listOf("Triceps")
                    )
                )

                // Pull exercises
                val latPulldownId = dao.insertExercise(
                    EntityExercise(
                        name = "Lat Pulldown",
                        sets = 4,
                        reps = 10,
                        weight = 50,
                        muscle = "Back",
                        part = listOf("Lats", "Upper Back")
                    )
                )
                val bicepCurlId = dao.insertExercise(
                    EntityExercise(
                        name = "Bicep Curl",
                        sets = 3,
                        reps = 12,
                        weight = 15,
                        muscle = "Arms",
                        part = listOf("Biceps")
                    )
                )

                // Legs exercises
                val squatId = dao.insertExercise(
                    EntityExercise(
                        name = "Barbell Squat",
                        sets = 4,
                        reps = 8,
                        weight = 80,
                        muscle = "Legs",
                        part = listOf("Quadriceps", "Glutes", "Hamstrings")
                    )
                )

                // Core & Cardio exercises
                val plankId = dao.insertExercise(
                    EntityExercise(
                        name = "Plank",
                        sets = 3,
                        reps = 1060, // 1 minute = 1060 (60 seconds + 1000 to indicate time)
                        weight = 0,
                        muscle = "Core",
                        part = listOf("Abs", "Lower Back")
                    )
                )
                val hiitId = dao.insertExercise(
                    EntityExercise(
                        name = "HIIT Running",
                        sets = 1,
                        reps = 1900, // 15 minutes = 1900 (900 seconds + 1000 to indicate time)
                        weight = 0,
                        muscle = "Cardio",
                        part = listOf("Full Body")
                    )
                )

            // Associate Exercises with Workouts
                // Push Day (3 exercises)
                dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(pushWorkoutId.toInt(), benchPressId.toInt()))
                dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(pushWorkoutId.toInt(), shoulderPressId.toInt()))
                dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(pushWorkoutId.toInt(), tricepExtensionId.toInt()))

                // Pull Day (2 exercises)
                dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(pullWorkoutId.toInt(), latPulldownId.toInt()))
                dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(pullWorkoutId.toInt(), bicepCurlId.toInt()))

                // Leg Day (1 exercise)
                dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(legsWorkoutId.toInt(), squatId.toInt()))

                // Quick Upper Body (2 exercises)
                dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(quickUpperId.toInt(), benchPressId.toInt()))
                dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(quickUpperId.toInt(), latPulldownId.toInt()))

                // Core & Cardio (2 exercises)
                dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(coreCardioId.toInt(), plankId.toInt()))
                dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(coreCardioId.toInt(), hiitId.toInt()))

                // Add 5 initial workout sessions, one day apart
                val currentTime = System.currentTimeMillis()
                val oneDayInMillis = 24 * 60 * 60 * 1000L

                // Session 1: Push Day (5 days ago)
                val session1Id = dao.insertWorkoutSession(
                    SessionWorkoutEntity(
                        sessionId = currentTime - (5 * oneDayInMillis),
                        workoutId = pushWorkoutId.toInt(),
                        startTime = currentTime - (5 * oneDayInMillis),
                        endTime = currentTime - (5 * oneDayInMillis) + (45 * 60 * 1000), // 45 minutes
                        workoutName = "Push Day"
                    )
                )
                // Add exercises for Push Day
                dao.insertExerciseSession(
                    SessionEntityExercise(
                        sessionId = session1Id,
                        exerciseId = benchPressId.toLong(),
                        sets = 4,
                        repsOrTime = listOf(8, 8, 8, 6),
                        weight = listOf(60, 60, 60, 55),
                        muscleGroup = "Chest",
                        rpe = 8,
                        subjectiveSoreness = 7,
                        eccentricFactor = 1.2f,
                        noveltyFactor = 5,
                        adaptationLevel = 7,
                        muscleParts = "Upper Chest, Middle Chest, Front Deltoids",
                        completedSets = 4,
                        notes = "Good form maintained throughout"
                    )
                )
                dao.insertExerciseSession(
                    SessionEntityExercise(
                        sessionId = session1Id,
                        exerciseId = shoulderPressId.toLong(),
                        sets = 3,
                        repsOrTime = listOf(10, 10, 8),
                        weight = listOf(40, 40, 35),
                        muscleGroup = "Shoulders",
                        rpe = 7,
                        subjectiveSoreness = 6,
                        eccentricFactor = 1.1f,
                        noveltyFactor = 4,
                        adaptationLevel = 6,
                        muscleParts = "Front Deltoids, Middle Deltoids",
                        completedSets = 3,
                        notes = "Focused on controlled movement"
                    )
                )

                // Session 2: Pull Day (4 days ago)
                val session2Id = dao.insertWorkoutSession(
                    SessionWorkoutEntity(
                        sessionId = currentTime - (4 * oneDayInMillis),
                        workoutId = pullWorkoutId.toInt(),
                        startTime = currentTime - (4 * oneDayInMillis),
                        endTime = currentTime - (4 * oneDayInMillis) + (40 * 60 * 1000), // 40 minutes
                        workoutName = "Pull Day"
                    )
                )
                // Add exercises for Pull Day
                dao.insertExerciseSession(
                    SessionEntityExercise(
                        sessionId = session2Id,
                        exerciseId = latPulldownId.toLong(),
                        sets = 4,
                        repsOrTime = listOf(10, 10, 10, 8),
                        weight = listOf(50, 50, 45, 45),
                        muscleGroup = "Back",
                        rpe = 8,
                        subjectiveSoreness = 7,
                        eccentricFactor = 1.3f,
                        noveltyFactor = 6,
                        adaptationLevel = 5,
                        muscleParts = "Lats, Upper Back",
                        completedSets = 4,
                        notes = "Full range of motion achieved"
                    )
                )

                // Session 3: Leg Day (3 days ago)
                val session3Id = dao.insertWorkoutSession(
                    SessionWorkoutEntity(
                        sessionId = currentTime - (3 * oneDayInMillis),
                        workoutId = legsWorkoutId.toInt(),
                        startTime = currentTime - (3 * oneDayInMillis),
                        endTime = currentTime - (3 * oneDayInMillis) + (50 * 60 * 1000), // 50 minutes
                        workoutName = "Leg Day"
                    )
                )
                // Add exercises for Leg Day
                dao.insertExerciseSession(
                    SessionEntityExercise(
                        sessionId = session3Id,
                        exerciseId = squatId.toLong(),
                        sets = 4,
                        repsOrTime = listOf(8, 8, 8, 6),
                        weight = listOf(80, 80, 75, 70),
                        muscleGroup = "Legs",
                        rpe = 9,
                        subjectiveSoreness = 8,
                        eccentricFactor = 1.4f,
                        noveltyFactor = 7,
                        adaptationLevel = 6,
                        muscleParts = "Quadriceps, Glutes, Hamstrings",
                        completedSets = 4,
                        notes = "Deep squats with good form"
                    )
                )

                // Session 4: Quick Upper Body (2 days ago)
                val session4Id = dao.insertWorkoutSession(
                    SessionWorkoutEntity(
                        sessionId = currentTime - (2 * oneDayInMillis),
                        workoutId = quickUpperId.toInt(),
                        startTime = currentTime - (2 * oneDayInMillis),
                        endTime = currentTime - (2 * oneDayInMillis) + (30 * 60 * 1000), // 30 minutes
                        workoutName = "Quick Upper Body"
                    )
                )
                // Add exercises for Quick Upper
                dao.insertExerciseSession(
                    SessionEntityExercise(
                        sessionId = session4Id,
                        exerciseId = benchPressId.toLong(),
                        sets = 3,
                        repsOrTime = listOf(8, 8, 6),
                        weight = listOf(55, 55, 50),
                        muscleGroup = "Chest",
                        rpe = 7,
                        subjectiveSoreness = 6,
                        eccentricFactor = 1.2f,
                        noveltyFactor = 4,
                        adaptationLevel = 7,
                        muscleParts = "Upper Chest, Middle Chest, Front Deltoids",
                        completedSets = 3,
                        notes = "Quick but effective session"
                    )
                )

                // Session 5: Core & Cardio (1 day ago)
                val session5Id = dao.insertWorkoutSession(
                    SessionWorkoutEntity(
                        sessionId = currentTime - oneDayInMillis,
                        workoutId = coreCardioId.toInt(),
                        startTime = currentTime - oneDayInMillis,
                        endTime = currentTime - oneDayInMillis + (35 * 60 * 1000), // 35 minutes
                        workoutName = "Core & Cardio"
                    )
                )
                // Add exercises for Core & Cardio
                dao.insertExerciseSession(
                    SessionEntityExercise(
                        sessionId = session5Id,
                        exerciseId = plankId.toLong(),
                        sets = 3,
                        repsOrTime = listOf(1060, 1060, 1060),
                        weight = listOf(0, 0, 0),
                        muscleGroup = "Core",
                        rpe = 8,
                        subjectiveSoreness = 7,
                        eccentricFactor = 1.0f,
                        noveltyFactor = 5,
                        adaptationLevel = 6,
                        muscleParts = "Abs, Lower Back",
                        completedSets = 3,
                        notes = "Maintained proper plank position"
                    )
                )
                dao.insertExerciseSession(
                    SessionEntityExercise(
                        sessionId = session5Id,
                        exerciseId = hiitId.toLong(),
                        sets = 1,
                        repsOrTime = listOf(1900),
                        weight = listOf(0),
                        muscleGroup = "Cardio",
                        rpe = 9,
                        subjectiveSoreness = 7,
                        eccentricFactor = 1.0f,
                        noveltyFactor = 6,
                        adaptationLevel = 5,
                        muscleParts = "Full Body",
                        completedSets = 1,
                        notes = "High intensity intervals maintained"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }
}