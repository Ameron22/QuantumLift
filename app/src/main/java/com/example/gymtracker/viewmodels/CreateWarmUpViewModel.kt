package com.example.gymtracker.viewmodels

import androidx.lifecycle.ViewModel
import com.example.gymtracker.data.WarmUpExercise
import com.example.gymtracker.data.EntityExercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CreateWarmUpViewModel : ViewModel() {
    
    // Template details state
    private val _templateName = MutableStateFlow("")
    val templateName: StateFlow<String> = _templateName.asStateFlow()
    
    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow("General")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()
    
    private val _selectedDifficulty = MutableStateFlow("Beginner")
    val selectedDifficulty: StateFlow<String> = _selectedDifficulty.asStateFlow()
    
    private val _estimatedDuration = MutableStateFlow(10)
    val estimatedDuration: StateFlow<Int> = _estimatedDuration.asStateFlow()
    
    private val _selectedMuscleGroups = MutableStateFlow<List<String>>(emptyList())
    val selectedMuscleGroups: StateFlow<List<String>> = _selectedMuscleGroups.asStateFlow()
    
    // Exercise list state
    private val _warmUpExercises = MutableStateFlow<List<WarmUpExercise>>(emptyList())
    val warmUpExercises: StateFlow<List<WarmUpExercise>> = _warmUpExercises.asStateFlow()
    
    private val _exerciseDetails = MutableStateFlow<Map<Int, EntityExercise>>(emptyMap())
    val exerciseDetails: StateFlow<Map<Int, EntityExercise>> = _exerciseDetails.asStateFlow()
    
    // Functions to update template details
    fun updateTemplateName(name: String) {
        _templateName.value = name
    }
    
    fun updateDescription(desc: String) {
        _description.value = desc
    }
    
    fun updateCategory(category: String) {
        _selectedCategory.value = category
    }
    
    fun updateDifficulty(difficulty: String) {
        _selectedDifficulty.value = difficulty
    }
    
    fun updateEstimatedDuration(duration: Int) {
        _estimatedDuration.value = duration
    }
    
    fun updateMuscleGroups(muscleGroups: List<String>) {
        _selectedMuscleGroups.value = muscleGroups
    }
    
    fun addMuscleGroup(muscleGroup: String) {
        if (!_selectedMuscleGroups.value.contains(muscleGroup)) {
            _selectedMuscleGroups.value = _selectedMuscleGroups.value + muscleGroup
        }
    }
    
    fun removeMuscleGroup(muscleGroup: String) {
        _selectedMuscleGroups.value = _selectedMuscleGroups.value.filter { it != muscleGroup }
    }
    
    // Functions to manage exercises
    fun addExercise(warmUpExercise: WarmUpExercise, exercise: EntityExercise) {
        _warmUpExercises.value = _warmUpExercises.value + warmUpExercise
        _exerciseDetails.value = _exerciseDetails.value + (exercise.id to exercise)
    }
    
    fun removeExercise(warmUpExercise: WarmUpExercise) {
        _warmUpExercises.value = _warmUpExercises.value.filter { it != warmUpExercise }
        _exerciseDetails.value = _exerciseDetails.value.filterKeys { it != warmUpExercise.exerciseId }
    }
    
    fun clearAllData() {
        _templateName.value = ""
        _description.value = ""
        _selectedCategory.value = "General"
        _selectedDifficulty.value = "Beginner"
        _estimatedDuration.value = 10
        _selectedMuscleGroups.value = emptyList()
        _warmUpExercises.value = emptyList()
        _exerciseDetails.value = emptyMap()
    }
    
    fun hasValidData(): Boolean {
        return _templateName.value.isNotEmpty() && _warmUpExercises.value.isNotEmpty()
    }
}

