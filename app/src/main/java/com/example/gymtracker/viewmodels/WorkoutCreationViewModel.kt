package com.example.gymtracker.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.Exercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkoutCreationViewModel : ViewModel() {
    private val _workoutName = MutableStateFlow("")
    val workoutName: StateFlow<String> = _workoutName.asStateFlow()

    private val _exercisesList = MutableStateFlow<List<Exercise>>(emptyList())
    val exercisesList: StateFlow<List<Exercise>> = _exercisesList.asStateFlow()

    fun updateWorkoutName(name: String) {
        println("WorkoutCreationViewModel: Updating workout name to: $name")
        _workoutName.value = name
    }

    fun addExercise(exercise: Exercise) {
        println("WorkoutCreationViewModel: Adding exercise: ${exercise.name}")
        println("WorkoutCreationViewModel: Current list size: ${_exercisesList.value.size}")
        val newList = _exercisesList.value.toMutableList()
        newList.add(exercise)
        _exercisesList.value = newList
        println("WorkoutCreationViewModel: New list size: ${_exercisesList.value.size}")
        _exercisesList.value.forEachIndexed { index, e ->
            println("WorkoutCreationViewModel: Exercise $index: ${e.name}")
        }
    }

    fun updateExercise(index: Int, exercise: Exercise) {
        println("WorkoutCreationViewModel: Updating exercise at index $index: ${exercise.name}")
        val newList = _exercisesList.value.toMutableList()
        newList[index] = exercise
        _exercisesList.value = newList
        println("WorkoutCreationViewModel: Updated list size: ${_exercisesList.value.size}")
    }

    fun removeExercise(index: Int) {
        println("WorkoutCreationViewModel: Removing exercise at index $index")
        val newList = _exercisesList.value.toMutableList()
        newList.removeAt(index)
        _exercisesList.value = newList
        println("WorkoutCreationViewModel: New list size: ${_exercisesList.value.size}")
    }

    fun clearWorkout() {
        println("WorkoutCreationViewModel: Clearing workout")
        _workoutName.value = ""
        _exercisesList.value = emptyList()
        println("WorkoutCreationViewModel: Workout cleared")
    }
} 