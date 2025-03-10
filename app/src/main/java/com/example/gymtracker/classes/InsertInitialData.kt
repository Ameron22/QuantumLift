package com.example.gymtracker.classes

import com.example.gymtracker.data.ExerciseDao
import com.example.gymtracker.data.ExerciseEntity
import com.example.gymtracker.data.ExerciseSessionEntity
import com.example.gymtracker.data.WorkoutEntity
import com.example.gymtracker.data.WorkoutExerciseCrossRef
import com.example.gymtracker.data.WorkoutSessionEntity

class InsertInitialData {
    suspend fun insertInitialData(dao: ExerciseDao) {
        // Check if the database is empty
        if (dao.getWorkoutCount() == 0) {
            // Insert Workouts
            val workout1Id = dao.insertWorkout(WorkoutEntity(name = "Full Body Workout"))
            val workout2Id = dao.insertWorkout(WorkoutEntity(name = "Upper Body Workout"))
            val workout3Id = dao.insertWorkout(WorkoutEntity(name = "Lower Body Workout"))
            val workout4Id = dao.insertWorkout(WorkoutEntity(name = "Cardio Workout"))
            val workout5Id = dao.insertWorkout(WorkoutEntity(name = "Core Workout"))

            // Insert Exercises
            val exercise1Id = dao.insertExercise(ExerciseEntity(name = "Push-ups", sets = 3, reps = 15, weight = 0, muscle = "Chest", part = listOf("Upper Chest", "Middle Chest")))
            val exercise2Id = dao.insertExercise(ExerciseEntity(name = "Pull-ups", sets = 3, reps = 10, weight = 0, muscle = "Back", part = listOf("Upper Back", "Lats")))
            val exercise3Id = dao.insertExercise(ExerciseEntity(name = "Squats", sets = 4, reps = 12, weight = 20, muscle = "Legs", part = listOf("Quadriceps", "Glutes")))
            val exercise4Id = dao.insertExercise(ExerciseEntity(name = "Plank", sets = 3, reps = 1940, weight = 0, muscle = "Core", part = listOf("Abs", "Obliques")))
            val exercise5Id = dao.insertExercise(ExerciseEntity(name = "Running", sets = 1, reps = 1800, weight = 0, muscle = "Cardio", part = listOf("Legs")))

            // Associate Exercises with Workouts
            dao.insertWorkoutExerciseCrossRef(WorkoutExerciseCrossRef(workout1Id.toInt(), exercise1Id.toInt()))
            dao.insertWorkoutExerciseCrossRef(WorkoutExerciseCrossRef(workout1Id.toInt(), exercise2Id.toInt()))
            dao.insertWorkoutExerciseCrossRef(WorkoutExerciseCrossRef(workout2Id.toInt(), exercise1Id.toInt()))
            dao.insertWorkoutExerciseCrossRef(WorkoutExerciseCrossRef(workout2Id.toInt(), exercise2Id.toInt()))
            dao.insertWorkoutExerciseCrossRef(WorkoutExerciseCrossRef(workout3Id.toInt(), exercise3Id.toInt()))
            dao.insertWorkoutExerciseCrossRef(WorkoutExerciseCrossRef(workout4Id.toInt(), exercise5Id.toInt()))
            dao.insertWorkoutExerciseCrossRef(WorkoutExerciseCrossRef(workout5Id.toInt(), exercise4Id.toInt()))

            // Insert Workout Sessions
            val session1Id = dao.insertWorkoutSession(WorkoutSessionEntity(workoutId = workout1Id.toInt(), startTime = System.currentTimeMillis() - 86400000, duration = 1800, workoutName = "Full Body Workout"))
            val session2Id = dao.insertWorkoutSession(WorkoutSessionEntity(workoutId = workout2Id.toInt(), startTime = System.currentTimeMillis() - 172800000, duration = 1500, workoutName = "Upper Body Workout"))
            val session3Id = dao.insertWorkoutSession(WorkoutSessionEntity(workoutId = workout3Id.toInt(), startTime = System.currentTimeMillis() - 259200000, duration = 1200, workoutName = "Lower Body Workout"))

            // Insert Exercise Sessions
            dao.insertExerciseSession(ExerciseSessionEntity(sessionId = session1Id, exerciseId = exercise1Id, sets = 3, repsOrTime = 15, muscleGroup = "Chest", muscleParts = listOf("Upper Chest", "Middle Chest"), completedSets = 3, completedRepsOrTime = 15, weight = 5))
            dao.insertExerciseSession(ExerciseSessionEntity(sessionId = session1Id, exerciseId = exercise2Id, sets = 3, repsOrTime = 10, muscleGroup = "Back", muscleParts = listOf("Upper Back", "Lats"), completedSets = 3, completedRepsOrTime = 10, weight = 5))
            dao.insertExerciseSession(ExerciseSessionEntity(sessionId = session2Id, exerciseId = exercise1Id, sets = 3, repsOrTime = 15, muscleGroup = "Chest", muscleParts = listOf("Upper Chest", "Middle Chest"), completedSets = 3, completedRepsOrTime = 15, weight = 5))
        }
    }
}