package com.example.gymtracker.utils

import android.content.Context
import android.util.Log
import com.example.gymtracker.data.CsvExercise
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.ExerciseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader

class ExerciseDataImporter(private val context: Context, private val dao: ExerciseDao) {
    private val TAG = "ExerciseDataImporter"

    suspend fun importExercises() {
        try {
            Log.d(TAG, "Starting exercise import")
            val exercises = readCsvFile()
            Log.d(TAG, "Read ${exercises.size} exercises from CSV")
            if (exercises.isEmpty()) {
                Log.e(TAG, "No exercises were read from the CSV file!")
                return
            }
            insertExercisesToDatabase(exercises)
            Log.d(TAG, "Successfully imported exercises")
        } catch (e: Exception) {
            Log.e(TAG, "Error importing exercises", e)
        }
    }

    private suspend fun readCsvFile(): List<CsvExercise> = withContext(Dispatchers.IO) {
        val exercises = mutableListOf<CsvExercise>()
        try {
            Log.d(TAG, "Opening CSV file")
            val csvFileName = "exercises_english_manual.csv"
            val files = context.assets.list("")
            Log.d(TAG, "Available files in assets: ${files?.joinToString()}")
            
            if (!files?.contains(csvFileName)!!) {
                Log.e(TAG, "CSV file not found in assets!")
                return@withContext exercises
            }

            context.assets.open(csvFileName).use { inputStream ->
                BufferedReader(inputStream.reader()).use { reader ->
                    // Skip header line
                    val header = reader.readLine()
                    Log.d(TAG, "CSV Header: $header")
                    
                    var lineCount = 0
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        lineCount++
                        val parts = line!!.split(",")
                        Log.d(TAG, "Processing line $lineCount: ${parts.joinToString()}")
                        
                        if (parts.size >= 8) {
                            try {
                                val id = parts[0].toIntOrNull() ?: 0
                                val title = parts[1]
                                
                                // Construct GIF filename using ID and title
                                val gifFilename = String.format("%04d_%s.gif", id, title.replace(" ", "_"))
                                val gifPath = "exercise_gifs/$gifFilename"
                                
                                Log.d(TAG, "Exercise ID: $id")
                                Log.d(TAG, "Exercise Title: $title")
                                Log.d(TAG, "GIF filename: $gifFilename")
                                Log.d(TAG, "GIF path: $gifPath")
                                
                                val exercise = CsvExercise(
                                    id = id,
                                    title = title,
                                    category = parts[2],
                                    muscleGroup = parts[3],
                                    muscles = parts[4].split(", "),
                                    equipment = parts[5],
                                    difficulty = parts[6],
                                    gifUrl = gifPath
                                )
                                exercises.add(exercise)
                                Log.d(TAG, "Successfully parsed exercise: ${exercise.title}")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing line $lineCount: ${e.message}")
                            }
                        } else {
                            Log.e(TAG, "Invalid line format at line $lineCount: ${parts.size} parts found, expected 8")
                        }
                    }
                    Log.d(TAG, "Finished reading CSV file. Total lines processed: $lineCount")
                }
            }
            Log.d(TAG, "Successfully read CSV file. Total exercises parsed: ${exercises.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error reading CSV file", e)
        }
        exercises
    }

    private suspend fun insertExercisesToDatabase(exercises: List<CsvExercise>) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting database insertion")
            var insertedCount = 0
            exercises.forEach { csvExercise ->
                try {
                    val exercise = EntityExercise(
                        name = csvExercise.title,
                        sets = 3, // Default value
                        reps = 12, // Default value
                        weight = 0, // Default value
                        muscle = csvExercise.muscleGroup,
                        part = csvExercise.muscles,
                        gifUrl = csvExercise.gifUrl
                    )
                    dao.insertExercise(exercise)
                    insertedCount++
                    Log.d(TAG, "Inserted exercise: ${exercise.name} with GIF path: ${exercise.gifUrl}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting exercise ${csvExercise.title}: ${e.message}")
                }
            }
            Log.d(TAG, "Successfully inserted $insertedCount exercises into database")
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting exercises into database", e)
        }
    }
} 