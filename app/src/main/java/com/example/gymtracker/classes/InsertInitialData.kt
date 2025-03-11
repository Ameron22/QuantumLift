package com.example.gymtracker.classes

import com.example.gymtracker.data.ExerciseDao
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.SessionEntityExercise
import com.example.gymtracker.data.EntityWorkout
import com.example.gymtracker.data.CrossRefWorkoutExercise
import com.example.gymtracker.data.SessionWorkoutEntity

class InsertInitialData {
    suspend fun insertInitialData(dao: ExerciseDao) {
        // Check if the database is empty
        if (dao.getWorkoutCount() == 0) {
            // Insert Workouts
            val workout1Id = dao.insertWorkout(EntityWorkout(name = "Full Body Workout"))
            val workout2Id = dao.insertWorkout(EntityWorkout(name = "Upper Body Workout"))
            val workout3Id = dao.insertWorkout(EntityWorkout(name = "Lower Body Workout"))
            val workout4Id = dao.insertWorkout(EntityWorkout(name = "Cardio Workout"))
            val workout5Id = dao.insertWorkout(EntityWorkout(name = "Core Workout"))

            // Insert Exercises
            val exercise1Id = dao.insertExercise(EntityExercise(name = "Push-ups", sets = 3, reps = 15, weight = 0, muscle = "Chest", part = listOf("Upper Chest", "Middle Chest")))
            val exercise2Id = dao.insertExercise(EntityExercise(name = "Pull-ups", sets = 3, reps = 10, weight = 0, muscle = "Back", part = listOf("Upper Back", "Lats")))
            val exercise3Id = dao.insertExercise(EntityExercise(name = "Squats", sets = 4, reps = 12, weight = 20, muscle = "Legs", part = listOf("Quadriceps", "Glutes")))
            val exercise4Id = dao.insertExercise(EntityExercise(name = "Plank", sets = 3, reps = 1940, weight = 0, muscle = "Core", part = listOf("Abs", "Obliques")))
            val exercise5Id = dao.insertExercise(EntityExercise(name = "Running", sets = 1, reps = 1800, weight = 0, muscle = "Cardio", part = listOf("Legs")))

            // Associate Exercises with Workouts
            dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(workout1Id.toInt(), exercise1Id.toInt()))
            dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(workout1Id.toInt(), exercise2Id.toInt()))
            dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(workout2Id.toInt(), exercise1Id.toInt()))
            dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(workout2Id.toInt(), exercise2Id.toInt()))
            dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(workout3Id.toInt(), exercise3Id.toInt()))
            dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(workout4Id.toInt(), exercise5Id.toInt()))
            dao.insertWorkoutExerciseCrossRef(CrossRefWorkoutExercise(workout5Id.toInt(), exercise4Id.toInt()))

            // Insert Workout Sessions
            val session1Id = dao.insertWorkoutSession(SessionWorkoutEntity(workoutId = workout1Id.toInt(), startTime = System.currentTimeMillis() - 86400000, duration = 1800, workoutName = "Full Body Workout"))
            val session2Id = dao.insertWorkoutSession(SessionWorkoutEntity(workoutId = workout2Id.toInt(), startTime = System.currentTimeMillis() - 172800000, duration = 1500, workoutName = "Upper Body Workout"))
            val session3Id = dao.insertWorkoutSession(SessionWorkoutEntity(workoutId = workout3Id.toInt(), startTime = System.currentTimeMillis() - 259200000, duration = 1200, workoutName = "Lower Body Workout"))

          /*  // Insert Exercise Sessions
            dao.insertExerciseSession(SessionEntityExercise(sessionId = session1Id, exerciseId = exercise1Id, sets = 3, repsOrTime = 15, muscleGroup = "Chest", muscleParts = listOf("Upper Chest", "Middle Chest"), completedSets = 3, completedRepsOrTime = 15, weight = 5))
            dao.insertExerciseSession(SessionEntityExercise(sessionId = session1Id, exerciseId = exercise2Id, sets = 3, repsOrTime = 10, muscleGroup = "Back", muscleParts = listOf("Upper Back", "Lats"), completedSets = 3, completedRepsOrTime = 10, weight = 5))
            dao.insertExerciseSession(SessionEntityExercise(sessionId = session2Id, exerciseId = exercise1Id, sets = 3, repsOrTime = 15, muscleGroup = "Chest", muscleParts = listOf("Upper Chest", "Middle Chest"), completedSets = 3, completedRepsOrTime = 15, weight = 5))
      */  }
    }
}