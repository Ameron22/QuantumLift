package com.example.gymtracker

import android.app.Application
import com.example.gymtracker.data.AppDatabase

class QuantumLiftApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}