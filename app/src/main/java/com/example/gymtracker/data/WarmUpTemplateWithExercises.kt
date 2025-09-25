package com.example.gymtracker.data

import androidx.room.Embedded
import androidx.room.Relation

data class WarmUpTemplateWithExercises(
    @Embedded val template: WarmUpTemplate,
    @Relation(
        parentColumn = "id",
        entityColumn = "templateId"
    )
    val exercises: List<WarmUpExercise>
)

