package com.example.gymtracker.utils

import android.content.Context
import android.util.Log
import com.example.gymtracker.data.CsvExercise
import com.example.gymtracker.data.EntityExercise
import com.example.gymtracker.data.ExerciseDao
import com.example.gymtracker.data.Converter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.StringReader

class ExerciseDataImporter(private val context: Context, private val dao: ExerciseDao) {
    private val TAG = "ExerciseDataImporter"

    suspend fun importExercises() {
        try {
            Log.d(TAG, "Starting exercise import")
            
            // Check if database is empty
            val existingCount = dao.getExerciseCount()
            if (existingCount > 0) {
                Log.d(TAG, "Database already contains $existingCount exercises. Skipping import.")
                return
            }
            
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

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        
        while (i < line.length) {
            when (line[i]) {
                '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        // Handle escaped quotes
                        current.append('"')
                        i++ // Skip the next quote
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ',' -> {
                    if (inQuotes) {
                        current.append(',')
                    } else {
                        result.add(current.toString().trim())
                        current = StringBuilder()
                    }
                }
                else -> current.append(line[i])
            }
            i++
        }
        
        // Add the last field
        result.add(current.toString().trim())
        return result
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
                        Log.d(TAG, "Raw line $lineCount: $line")
                        val parts = parseCsvLine(line!!)
                        Log.d(TAG, "Parsed parts for line $lineCount: ${parts.joinToString(" | ")}")
                        
                        if (parts.size >= 9) {
                            try {
                                val id = parts[0].toIntOrNull() ?: continue
                                val title = parts[1]
                                val category = parts[2]
                                val muscleGroup = parts[3]
                                val muscles = parts[4].trim().split(", ").map { it.trim() }
                                val equipment = parts[5].trim().split(", ").map { it.trim() }.joinToString(", ")
                                val difficulty = parts[6]
                                // Construct GIF filename using ID and title
                                val gifFilename = String.format("%04d_%s.gif", id, title.replace(" ", "_"))
                                val gifPath = "exercise_gifs/$gifFilename"
                                // Parse use_time column (last column)
                                val useTime = parts.getOrNull(8)?.trim()?.lowercase() == "true"
                                Log.d(TAG, "Processing exercise $lineCount: ID=$id, Title=$title, Category=$category, MuscleGroup=$muscleGroup, Muscles=$muscles, Equipment=$equipment, Difficulty=$difficulty, UseTime=$useTime")
                                val exercise = CsvExercise(
                                    id = id,
                                    title = title,
                                    category = category,
                                    muscleGroup = muscleGroup,
                                    muscles = muscles,
                                    equipment = equipment,
                                    difficulty = difficulty,
                                    gifUrl = gifPath,
                                    useTime = useTime
                                )
                                exercises.add(exercise)
                                Log.d(TAG, "Difficulty - Successfully parsed exercise: ${exercise.title} with difficulty: ${exercise.difficulty} useTime: ${exercise.useTime}")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing line $lineCount: ${e.message}")
                                Log.e(TAG, "Line content: $line")
                                Log.e(TAG, "Parsed parts: ${parts.joinToString(" | ")}")
                            }
                        } else {
                            Log.e(TAG, "Invalid line format at line $lineCount: ${parts.size} parts found, expected 9")
                            Log.e(TAG, "Line content: $line")
                            Log.e(TAG, "Parsed parts: ${parts.joinToString(" | ")}")
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

    private fun convertDifficulty(difficulty: String): String {
        Log.d(TAG, "Difficulty - Converting value: '$difficulty'")
        return when (difficulty.trim()) {
            "1/3" -> {
                Log.d(TAG, "Difficulty - Converting to Beginner")
                "Beginner"
            }
            "2/3" -> {
                Log.d(TAG, "Difficulty - Converting to Intermediate")
                "Intermediate"
            }
            "3/3" -> {
                Log.d(TAG, "Difficulty - Converting to Advanced")
                "Advanced"
            }
            else -> {
                Log.w(TAG, "Difficulty - Unknown value: '$difficulty', defaulting to Intermediate")
                "Intermediate"
            }
        }
    }

    private suspend fun insertExercisesToDatabase(exercises: List<CsvExercise>) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting database insertion")
            var insertedCount = 0
            var skippedCount = 0
            var beginnerCount = 0
            var intermediateCount = 0
            var advancedCount = 0
            
            // Get all existing exercises
            val existingExercises = dao.getAllExercises()
            val existingExerciseNames = existingExercises.map { it.name }.toSet()
            
            exercises.forEach { csvExercise ->
                try {
                    // Skip if exercise already exists
                    if (existingExerciseNames.contains(csvExercise.title)) {
                        skippedCount++
                        Log.d(TAG, "Skipping existing exercise: ${csvExercise.title}")
                        return@forEach
                    }

                    val convertedDifficulty = convertDifficulty(csvExercise.difficulty)
                    when (convertedDifficulty) {
                        "Beginner" -> beginnerCount++
                        "Intermediate" -> intermediateCount++
                        "Advanced" -> advancedCount++
                    }
                    
                    val exercise = EntityExercise(
                        name = csvExercise.title,
                        muscle = csvExercise.muscleGroup,
                        parts = Converter().fromList(csvExercise.muscles),
                        equipment = csvExercise.equipment,
                        difficulty = convertedDifficulty,
                        gifUrl = csvExercise.gifUrl,
                        useTime = csvExercise.useTime
                    )
                    
                    dao.insertExercise(exercise)
                    insertedCount++
                    Log.d(TAG, "Difficulty - Inserted exercise: ${exercise.name} with difficulty: $convertedDifficulty")
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting exercise ${csvExercise.title}: ${e.message}")
                }
            }
            Log.d(TAG, "Database update complete: $insertedCount new exercises inserted, $skippedCount existing exercises skipped")
            Log.d(TAG, "Difficulty distribution: Beginner=$beginnerCount, Intermediate=$intermediateCount, Advanced=$advancedCount")
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting exercises into database", e)
        }
    }
} 