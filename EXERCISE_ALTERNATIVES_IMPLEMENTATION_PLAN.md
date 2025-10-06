# Exercise Alternatives Feature Implementation Plan

## Overview
This feature allows users to swipe left on exercise cards to access alternative exercises, enabling them to substitute exercises during workouts (e.g., switching from dumbbell bicep curls to cable bicep curls).

## Database Changes Made

### 1. New Entity: ExerciseAlternative
- **File**: `app/src/main/java/com/example/gymtracker/data/ExerciseAlternative.kt`
- **Purpose**: Stores alternative exercises for each workout exercise
- **Fields**:
  - `originalExerciseId`: The exercise being replaced
  - `alternativeExerciseId`: The alternative exercise
  - `workoutExerciseId`: The specific workout exercise instance
  - `order`: Order of alternatives
  - `isActive`: Whether this alternative is currently selected

### 2. Updated WorkoutExercise Entity
- **File**: `app/src/main/java/com/example/gymtracker/data/WorkoutExercise.kt`
- **New Field**: `hasAlternatives: Boolean = false`
- **Purpose**: Flag to indicate if alternatives exist for this exercise

### 3. Database Version Update
- **File**: `app/src/main/java/com/example/gymtracker/data/AppDatabase.kt`
- **Version**: Incremented to 48
- **Added**: ExerciseAlternative entity to the database

### 4. New DAO Methods
- **File**: `app/src/main/java/com/example/gymtracker/data/ExerciseDao.kt`
- **Added Methods**:
  - `insertExerciseAlternative()`
  - `updateExerciseAlternative()`
  - `deleteExerciseAlternative()`
  - `getExerciseAlternatives()`
  - `deactivateAllAlternatives()`
  - `activateAlternative()`
  - `updateWorkoutExerciseId()`
  - `updateWorkoutExerciseHasAlternatives()`
  - `getSimilarExercises()` - Find exercises by muscle group and equipment
  - `getExercisesByMuscleGroup()` - Find exercises by muscle group only
  - `getExerciseAlternativesWithDetails()` - Get alternatives with exercise details

## UI Components Created

### 1. ExerciseAlternativeDialog
- **File**: `app/src/main/java/com/example/gymtracker/components/ExerciseAlternativeDialog.kt`
- **Purpose**: Bottom sheet dialog for selecting/managing exercise alternatives
- **Features**:
  - Shows current exercise
  - Lists existing alternatives
  - Shows similar exercises if no alternatives exist
  - Add new alternative button
  - Visual indication of active alternative

### 2. SwipeableExerciseCard
- **File**: `app/src/main/java/com/example/gymtracker/components/SwipeableExerciseCard.kt`
- **Purpose**: Wrapper for exercise cards that handles horizontal swipe gestures
- **Features**:
  - Left swipe shows "Alternatives" action
  - Right swipe shows "Options" action (for future use)
  - Visual feedback during swipe
  - Only shows actions if alternatives exist

## Integration Steps for WorkoutDetailsScreen

### 1. Add State Variables
```kotlin
// Add these to WorkoutDetailsScreen state variables
var showAlternativeDialog by remember { mutableStateOf(false) }
var selectedWorkoutExercise by remember { mutableStateOf<WorkoutExerciseWithDetails?>(null) }
var exerciseAlternatives by remember { mutableStateOf<List<ExerciseAlternativeWithDetails>>(emptyList()) }
```

### 2. Add Functions
```kotlin
// Function to load alternatives for an exercise
fun loadExerciseAlternatives(workoutExerciseId: Int) {
    coroutineScope.launch {
        try {
            val alternatives = dao.getExerciseAlternativesWithDetails(workoutExerciseId)
            exerciseAlternatives = alternatives
        } catch (e: Exception) {
            Log.e("WorkoutDetailsScreen", "Error loading alternatives: ${e.message}")
        }
    }
}

// Function to add an alternative
fun addExerciseAlternative(originalExerciseId: Int, alternativeExerciseId: Int, workoutExerciseId: Int) {
    coroutineScope.launch {
        try {
            val maxOrder = dao.getExerciseAlternatives(workoutExerciseId).maxOfOrNull { it.order } ?: 0
            val alternative = ExerciseAlternative(
                originalExerciseId = originalExerciseId,
                alternativeExerciseId = alternativeExerciseId,
                workoutExerciseId = workoutExerciseId,
                order = maxOrder + 1,
                isActive = false
            )
            dao.insertExerciseAlternative(alternative)
            
            // Update hasAlternatives flag
            dao.updateWorkoutExerciseHasAlternatives(workoutExerciseId, true)
            
            // Reload alternatives
            loadExerciseAlternatives(workoutExerciseId)
        } catch (e: Exception) {
            Log.e("WorkoutDetailsScreen", "Error adding alternative: ${e.message}")
        }
    }
}

// Function to switch to an alternative
fun switchToAlternative(alternative: ExerciseAlternativeWithDetails) {
    coroutineScope.launch {
        try {
            // Deactivate all alternatives for this workout exercise
            dao.deactivateAllAlternatives(alternative.alternative.workoutExerciseId)
            
            // Activate the selected alternative
            dao.activateAlternative(alternative.alternative.id)
            
            // Update the workout exercise to use the alternative
            dao.updateWorkoutExerciseId(
                alternative.alternative.workoutExerciseId,
                alternative.alternative.alternativeExerciseId
            )
            
            // Reload the exercise list
            // (You'll need to refresh the reorderedExercises list)
            
            showAlternativeDialog = false
        } catch (e: Exception) {
            Log.e("WorkoutDetailsScreen", "Error switching alternative: ${e.message}")
        }
    }
}
```

### 3. Update ExerciseItem Composable
Replace the existing Card in ExerciseItem with SwipeableExerciseCard:

```kotlin
// In ExerciseItem composable, replace the Card with:
SwipeableExerciseCard(
    exercise = exercise,
    workoutExercise = workoutExercise,
    hasAlternatives = workoutExercise.hasAlternatives,
    onSwipeLeft = {
        selectedWorkoutExercise = WorkoutExerciseWithDetails(workoutExercise, exercise)
        loadExerciseAlternatives(workoutExercise.id)
        showAlternativeDialog = true
    },
    onSwipeRight = {
        // Future: Could show other options like edit, delete, etc.
    }
) {
    // Your existing card content goes here
    // (The Row with exercise details, etc.)
}
```

### 4. Add Dialog to Main Content
Add this at the end of your main content, before the closing brace:

```kotlin
// Exercise Alternative Dialog
if (showAlternativeDialog && selectedWorkoutExercise != null) {
    ExerciseAlternativeDialog(
        currentExercise = selectedWorkoutExercise!!.entityExercise,
        workoutExerciseId = selectedWorkoutExercise!!.workoutExercise.id,
        alternatives = exerciseAlternatives,
        onDismiss = { 
            showAlternativeDialog = false
            selectedWorkoutExercise = null
        },
        onSelectAlternative = { alternativeExercise ->
            // Find the alternative in the list and switch to it
            val alternative = exerciseAlternatives.find { 
                it.exercise.id == alternativeExercise.id 
            }
            alternative?.let { switchToAlternative(it) }
        },
        onAddAlternative = {
            // Navigate to exercise selection screen
            // You can reuse your existing AddExerciseToWorkoutScreen
            // but modify it to add as alternative instead
        },
        dao = dao
    )
}
```

## Usage Flow

1. **User swipes left** on an exercise card
2. **Dialog opens** showing:
   - Current exercise info
   - Existing alternatives (if any)
   - Similar exercises (if no alternatives exist)
   - Add new alternative button
3. **User can**:
   - Select an existing alternative to switch to it
   - Add a new alternative from similar exercises
   - Add a completely new alternative via the + button
4. **System updates**:
   - Database with new alternative relationships
   - UI to reflect the change
   - Exercise card shows the new exercise

## Benefits

1. **Flexibility**: Users can adapt workouts on the fly
2. **Equipment Substitution**: Switch between dumbbells, cables, machines, etc.
3. **Injury Adaptation**: Replace exercises that cause discomfort
4. **Progressive Overload**: Switch to harder/easier variations
5. **Gym Availability**: Adapt when equipment is unavailable

## Future Enhancements

1. **Smart Suggestions**: AI-powered exercise recommendations
2. **Difficulty Matching**: Automatically suggest alternatives of similar difficulty
3. **Muscle Group Filtering**: Show only exercises targeting the same muscles
4. **Equipment Preferences**: Remember user's preferred equipment types
5. **Workout History**: Suggest alternatives based on past performance

## Testing Considerations

1. **Database Migration**: Test that existing workouts work with new schema
2. **Swipe Gestures**: Ensure they don't interfere with existing drag-to-reorder
3. **Performance**: Test with many alternatives and large exercise databases
4. **UI Responsiveness**: Ensure smooth animations and feedback
5. **Data Consistency**: Verify that switching alternatives updates all related data correctly

