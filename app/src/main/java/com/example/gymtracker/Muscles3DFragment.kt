package com.example.gymtracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.app.Fragment
import com.google.android.filament.utils.Utils
import com.google.android.filament.utils.ModelViewer

class Muscles3DFragment : Fragment() {

    companion object {
        init { Utils.init() }
    }

    private lateinit var surfaceView: SurfaceView
    private lateinit var modelViewer: ModelViewer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_muscles_3d, container, false)
        surfaceView = root.findViewById(R.id.surfaceView)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ModelViewer
        modelViewer = ModelViewer(surfaceView)

        // Load a 3D model
        loadModel()
    }

    private fun loadModel() {
        try {
            val currentActivity = activity
            if (currentActivity == null) {
                return
            }

            // Load the muscles model from assets
            val buffer = currentActivity.assets.open("models/model_muscles.glb").use { input ->
                val bytes = ByteArray(input.available())
                input.read(bytes)
                java.nio.ByteBuffer.wrap(bytes)
            }

            modelViewer.loadModelGltfAsync(buffer) { uri ->
                currentActivity.assets.open("models/$uri").use { input ->
                    val bytes = ByteArray(input.available())
                    input.read(bytes)
                    java.nio.ByteBuffer.wrap(bytes)
                }
            }
        } catch (e: Exception) {
            // Handle error - model file might not exist
            e.printStackTrace()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up ModelViewer resources
        modelViewer.destroyModel()
    }


}