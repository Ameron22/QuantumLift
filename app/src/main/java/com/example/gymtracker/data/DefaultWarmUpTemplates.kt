package com.example.gymtracker.data

import com.example.gymtracker.data.Converter

object DefaultWarmUpTemplates {
    
    fun getDefaultTemplates(): List<WarmUpTemplate> {
        return listOf(
            // General Full Body Warm-Up
            WarmUpTemplate(
                name = "General Full Body",
                description = "A comprehensive warm-up routine that prepares your entire body for any workout",
                category = "General",
                targetMuscleGroups = Converter().fromList(listOf("Full Body")),
                difficulty = "Beginner",
                estimatedDuration = 10,
                isDefault = true,
                createdBy = "system"
            ),
            
            // Quick Cardio Warm-Up
            WarmUpTemplate(
                name = "Quick Cardio",
                description = "A fast-paced cardio warm-up to get your heart rate up and blood flowing",
                category = "Cardio",
                targetMuscleGroups = Converter().fromList(listOf("Cardio")),
                difficulty = "Beginner",
                estimatedDuration = 5,
                isDefault = true,
                createdBy = "system"
            ),
            
            // Upper Body Focus
            WarmUpTemplate(
                name = "Upper Body Focus",
                description = "Targeted warm-up for chest, back, shoulders, and arms",
                category = "Muscle-Specific",
                targetMuscleGroups = Converter().fromList(listOf("Chest", "Back", "Shoulders", "Arms")),
                difficulty = "Beginner",
                estimatedDuration = 8,
                isDefault = true,
                createdBy = "system"
            ),
            
            // Lower Body Focus
            WarmUpTemplate(
                name = "Lower Body Focus",
                description = "Warm-up routine specifically designed for legs and glutes",
                category = "Muscle-Specific",
                targetMuscleGroups = Converter().fromList(listOf("Legs", "Glutes")),
                difficulty = "Beginner",
                estimatedDuration = 8,
                isDefault = true,
                createdBy = "system"
            ),
            
            // Core Activation
            WarmUpTemplate(
                name = "Core Activation",
                description = "Activate and warm up your core muscles before strength training",
                category = "Muscle-Specific",
                targetMuscleGroups = Converter().fromList(listOf("Core", "Abs", "Lower Back")),
                difficulty = "Beginner",
                estimatedDuration = 6,
                isDefault = true,
                createdBy = "system"
            ),
            
            // Dynamic Stretching
            WarmUpTemplate(
                name = "Dynamic Stretching",
                description = "Improve flexibility and range of motion with dynamic stretches",
                category = "Stretching",
                targetMuscleGroups = Converter().fromList(listOf("Full Body")),
                difficulty = "Beginner",
                estimatedDuration = 12,
                isDefault = true,
                createdBy = "system"
            ),
            
            // Advanced Mobility
            WarmUpTemplate(
                name = "Advanced Mobility",
                description = "Advanced mobility work for experienced athletes",
                category = "Stretching",
                targetMuscleGroups = Converter().fromList(listOf("Full Body")),
                difficulty = "Advanced",
                estimatedDuration = 15,
                isDefault = true,
                createdBy = "system"
            )
        )
    }
    
    fun getDefaultExercisesForTemplate(templateId: Int): List<WarmUpExercise> {
        return when (templateId) {
            1 -> { // General Full Body
                listOf(
                    WarmUpExercise(templateId = 1, exerciseId = 1, order = 1, sets = 1, reps = 10, duration = 30, isTimeBased = false, restBetweenSets = 0),
                    WarmUpExercise(templateId = 1, exerciseId = 2, order = 2, sets = 1, reps = 10, duration = 30, isTimeBased = false, restBetweenSets = 0),
                    WarmUpExercise(templateId = 1, exerciseId = 3, order = 3, sets = 1, reps = 10, duration = 30, isTimeBased = false, restBetweenSets = 0),
                    WarmUpExercise(templateId = 1, exerciseId = 4, order = 4, sets = 1, reps = 10, duration = 30, isTimeBased = false, restBetweenSets = 0),
                    WarmUpExercise(templateId = 1, exerciseId = 5, order = 5, sets = 1, reps = 10, duration = 30, isTimeBased = false, restBetweenSets = 0)
                )
            }
            2 -> { // Quick Cardio
                listOf(
                    WarmUpExercise(templateId = 2, exerciseId = 6, order = 1, sets = 1, reps = 0, duration = 60, isTimeBased = true, restBetweenSets = 30),
                    WarmUpExercise(templateId = 2, exerciseId = 7, order = 2, sets = 1, reps = 0, duration = 60, isTimeBased = true, restBetweenSets = 30),
                    WarmUpExercise(templateId = 2, exerciseId = 8, order = 3, sets = 1, reps = 0, duration = 60, isTimeBased = true, restBetweenSets = 30)
                )
            }
            3 -> { // Upper Body Focus
                listOf(
                    WarmUpExercise(templateId = 3, exerciseId = 9, order = 1, sets = 1, reps = 12, duration = 0, isTimeBased = false, restBetweenSets = 0),
                    WarmUpExercise(templateId = 3, exerciseId = 10, order = 2, sets = 1, reps = 12, duration = 0, isTimeBased = false, restBetweenSets = 0),
                    WarmUpExercise(templateId = 3, exerciseId = 11, order = 3, sets = 1, reps = 12, duration = 0, isTimeBased = false, restBetweenSets = 0)
                )
            }
            4 -> { // Lower Body Focus
                listOf(
                    WarmUpExercise(templateId = 4, exerciseId = 12, order = 1, sets = 1, reps = 15, duration = 0, isTimeBased = false, restBetweenSets = 0),
                    WarmUpExercise(templateId = 4, exerciseId = 13, order = 2, sets = 1, reps = 15, duration = 0, isTimeBased = false, restBetweenSets = 0),
                    WarmUpExercise(templateId = 4, exerciseId = 14, order = 3, sets = 1, reps = 15, duration = 0, isTimeBased = false, restBetweenSets = 0)
                )
            }
            5 -> { // Core Activation
                listOf(
                    WarmUpExercise(templateId = 5, exerciseId = 15, order = 1, sets = 1, reps = 0, duration = 45, isTimeBased = true, restBetweenSets = 15),
                    WarmUpExercise(templateId = 5, exerciseId = 16, order = 2, sets = 1, reps = 0, duration = 45, isTimeBased = true, restBetweenSets = 15),
                    WarmUpExercise(templateId = 5, exerciseId = 17, order = 3, sets = 1, reps = 0, duration = 45, isTimeBased = true, restBetweenSets = 15)
                )
            }
            6 -> { // Dynamic Stretching
                listOf(
                    WarmUpExercise(templateId = 6, exerciseId = 18, order = 1, sets = 1, reps = 0, duration = 60, isTimeBased = true, restBetweenSets = 20),
                    WarmUpExercise(templateId = 6, exerciseId = 19, order = 2, sets = 1, reps = 0, duration = 60, isTimeBased = true, restBetweenSets = 20),
                    WarmUpExercise(templateId = 6, exerciseId = 20, order = 3, sets = 1, reps = 0, duration = 60, isTimeBased = true, restBetweenSets = 20)
                )
            }
            7 -> { // Advanced Mobility
                listOf(
                    WarmUpExercise(templateId = 7, exerciseId = 21, order = 1, sets = 1, reps = 0, duration = 90, isTimeBased = true, restBetweenSets = 30),
                    WarmUpExercise(templateId = 7, exerciseId = 22, order = 2, sets = 1, reps = 0, duration = 90, isTimeBased = true, restBetweenSets = 30),
                    WarmUpExercise(templateId = 7, exerciseId = 23, order = 3, sets = 1, reps = 0, duration = 90, isTimeBased = true, restBetweenSets = 30)
                )
            }
            else -> emptyList()
        }
    }
}

