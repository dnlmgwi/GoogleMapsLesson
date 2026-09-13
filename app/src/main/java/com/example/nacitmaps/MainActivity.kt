package com.example.nacitmaps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.nacitmaps.ui.theme.NacitMapsTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: Draw the app behind the status and navigation bars.
        enableEdgeToEdge()

        // Step 2: Set up the Compose UI for this screen.
        setContent {
            // Step 3: Apply the app theme (colors, fonts).
            NacitMapsTheme {
                // Step 4: Scaffold gives us padding so the map doesn't sit under the system bars.
                Scaffold { paddingValues ->

                    // Step 5: Pick where the camera starts: the center of Malawi, zoom level 6.
                    //         Zoom goes from 1 (whole world) to about 20 (single buildings).
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(LatLng(-13.25, 34.3), 6f)
                    }

                    // Step 6: Show the map. It fills the screen and uses the camera from step 5.
                    //         The API key is read from the manifest (com.google.android.geo.API_KEY).
                    GoogleMap(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        cameraPositionState = cameraPositionState,
                    )
                }
            }
        }
    }
}
