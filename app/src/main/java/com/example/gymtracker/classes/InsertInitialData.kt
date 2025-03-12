package com.example.gymtracker.classes

import com.example.gymtracker.data.ExerciseDao
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.EntityWorkout
import com.example.gymtracker.data.CrossRefWorkoutExercise

class InsertInitialData {
    suspend fun insertInitialData(dao: ExerciseDao) {
        // Check if the database is empty
        if (dao.getWorkoutCount() == 0) {
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
        }
    }
}