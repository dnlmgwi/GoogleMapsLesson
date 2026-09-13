# NacitMaps — Google Maps in Jetpack Compose

A step-by-step lesson for showing a Google Map in an Android app with Jetpack Compose, then adding markers to it.

The map is centred on Malawi. Everything below matches the code already in this project.

---

## What you need

- Android Studio (latest stable)
- An Android device or emulator **with Google Play services** (pick a "Google APIs" or "Google Play" system image)
- A Google account, to create a Maps API key

---

## Part 1 — Show a Google Map

### Step 1: Get a Maps API key

1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Create a new project (or pick an existing one).
3. Open **APIs & Services → Library**, search for **Maps SDK for Android**, and click **Enable**.
4. Open **APIs & Services → Credentials → Create credentials → API key**.
5. Copy the key.
6. (Recommended) Click the key and **restrict** it:
   - *Application restrictions* → **Android apps** → add the package name `com.example.nacitmaps` and your SHA-1 fingerprint.
     Get the SHA-1 by running `./gradlew signingReport` in the project root.
   - *API restrictions* → **Maps SDK for Android**.

> Billing must be enabled on the Cloud project for the map to load, even though the mobile Maps SDK has a free usage tier.

### Step 2: Store the key safely in `.env`

Never paste the key directly into code or the manifest — it would end up in git.

Create a file called `.env` in the **project root** (next to `settings.gradle.kts`):

```properties
MAPS_API_KEY=YOUR_REAL_KEY_HERE
```

`.env` is already listed in `.gitignore`, so it is never committed.

The project also has `local.defaults.properties`, which **is** committed. It holds a fake fallback so the project still builds for anyone who hasn't created `.env` yet:

```properties
MAPS_API_KEY=DEFAULT_API_KEY
```

(With the default key the app builds, but the map shows up blank/grey.)

### Step 3: Declare the dependencies and plugins

Versions live in the version catalog, `gradle/libs.versions.toml`:

```toml
[versions]
secrets-gradle-plugin = "2.0.1"
maps-compose = "6.10.0"

[libraries]
maps-compose = { module = "com.google.maps.android:maps-compose", version.ref = "maps-compose" }

[plugins]
secrets-gradle-plugin = { id = "com.google.android.libraries.mapsplatform.secrets-gradle-plugin", version.ref = "secrets-gradle-plugin" }
```

- **maps-compose** — the `GoogleMap`, `Marker`, etc. composables. It pulls in the Play services Maps SDK for you.
- **Secrets Gradle Plugin** — reads `MAPS_API_KEY` from `.env` and makes it available to the manifest.

### Step 4: Apply them in `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.secrets.gradle.plugin)   // 1. apply the secrets plugin
}

android {
    // ...
    buildFeatures {
        compose = true
        buildConfig = true                       // 2. secrets plugin also writes to BuildConfig
    }
}

dependencies {
    // ...
    implementation(libs.maps.compose)            // 3. Google Maps for Compose
}

secrets {
    // 4. Read the real key from .env ...
    propertiesFileName = ".env"
    // ... and fall back to this committed file if .env is missing.
    defaultPropertiesFileName = "local.defaults.properties"
}
```

Click **Sync Now** in Android Studio.

### Step 5: Pass the key to the Maps SDK in `AndroidManifest.xml`

Inside the `<application>` tag:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}"/>
```

At build time the secrets plugin replaces `${MAPS_API_KEY}` with the value from `.env`.

No `INTERNET` permission line is needed — the Maps SDK's own manifest adds it.

### Step 6: Draw the map in `MainActivity.kt`

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NacitMapsTheme {
                Scaffold { paddingValues ->

                    // Where the camera starts: centre of Malawi, zoom 6.
                    // Zoom: 1 = whole world, ~20 = single buildings.
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(LatLng(-13.25, 34.3), 6f)
                    }

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
```

Imports used:

```kotlin
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
```

### Step 7: Run it

Press **Run ▶**. You should see a map of Malawi you can pan and pinch-zoom.

**Map is blank/grey?**
- `.env` is missing or the key is wrong → check the file name and the `MAPS_API_KEY=` spelling.
- Maps SDK for Android isn't enabled, or billing is off, on the Cloud project.
- The key is restricted to a different package name or SHA-1.
- The emulator image has no Google Play services.
- Check **Logcat** and filter for `Google Android Maps SDK` — it prints the exact auth error.

---

## Part 2 — Add markers

A marker is a pin at a latitude/longitude. In Compose you add markers by putting `Marker` composables **inside** the `GoogleMap { ... }` content lambda.

### Step 1: Add a single marker

Change the `GoogleMap(...)` call so it has a trailing `{ }` block, and put a `Marker` in it:

```kotlin
GoogleMap(
    modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
    cameraPositionState = cameraPositionState,
) {
    Marker(
        state = rememberUpdatedMarkerState(position = LatLng(-13.9626, 33.7741)),
        title = "Lilongwe",
        snippet = "Capital city of Malawi",
    )
}
```

New imports:

```kotlin
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberUpdatedMarkerState
```

- `state` — holds the marker's position. `rememberUpdatedMarkerState` keeps the same state across recompositions.
- `title` / `snippet` — shown in the info window when the user taps the pin.

Run the app and tap the pin to see the info window.

### Step 2: Add several markers from a list

Instead of copy-pasting `Marker` calls, keep your places in a list and loop over it.

At the top level of `MainActivity.kt` (outside the class):

```kotlin
data class Place(val name: String, val description: String, val position: LatLng)

val places = listOf(
    Place("Lilongwe", "Capital city", LatLng(-13.9626, 33.7741)),
    Place("Blantyre", "Commercial hub", LatLng(-15.7861, 35.0058)),
    Place("Mzuzu", "Northern region", LatLng(-11.4656, 34.0207)),
    Place("Zomba", "Former capital", LatLng(-15.3850, 35.3188)),
)
```

Inside `GoogleMap { ... }`:

```kotlin
places.forEach { place ->
    Marker(
        state = rememberUpdatedMarkerState(position = place.position),
        title = place.name,
        snippet = place.description,
    )
}
```

### Step 3: Change the marker colour

Use `BitmapDescriptorFactory` to pick a built-in hue:

```kotlin
Marker(
    state = rememberUpdatedMarkerState(position = LatLng(-13.9626, 33.7741)),
    title = "Lilongwe",
    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
)
```

```kotlin
import com.google.android.gms.maps.model.BitmapDescriptorFactory
```

Other hues: `HUE_RED`, `HUE_GREEN`, `HUE_BLUE`, `HUE_ORANGE`, `HUE_YELLOW`, `HUE_VIOLET`, `HUE_ROSE`, `HUE_CYAN`, `HUE_MAGENTA` — or any float from `0f` to `360f`.

> `BitmapDescriptorFactory` only works once the map has initialised, so call it inside the `GoogleMap { }` block, not at the top level of the file.

### Step 4: React when a marker or its info window is tapped

```kotlin
val context = LocalContext.current

Marker(
    state = rememberUpdatedMarkerState(position = place.position),
    title = place.name,
    snippet = place.description,
    onClick = {
        // return false = also do the default (show info window, centre camera)
        // return true  = you handled it, skip the default
        false
    },
    onInfoWindowClick = {
        Toast.makeText(context, "You opened ${place.name}", Toast.LENGTH_SHORT).show()
    },
)
```

```kotlin
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
```

Put `val context = LocalContext.current` **above** the `GoogleMap(...)` call.

### Step 5: Let the user add markers by tapping the map

Keep the tapped points in a state list, and add to it from `onMapClick`:

```kotlin
val tappedPoints = remember { mutableStateListOf<LatLng>() }

GoogleMap(
    modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
    cameraPositionState = cameraPositionState,
    onMapClick = { latLng -> tappedPoints.add(latLng) },
) {
    tappedPoints.forEach { point ->
        Marker(
            state = rememberUpdatedMarkerState(position = point),
            title = "Dropped pin",
            snippet = "%.4f, %.4f".format(point.latitude, point.longitude),
        )
    }
}
```

```kotlin
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
```

Because `tappedPoints` is Compose state, each tap recomposes the map and a new pin appears.

Tip: use `onMapLongClick` instead of `onMapClick` if normal taps should stay free for dismissing info windows.

### Step 6: Move the camera to a marker

`cameraPositionState` can animate to any position. For example, fly to Blantyre when its info window is tapped:

```kotlin
val scope = rememberCoroutineScope()

// inside Marker(...)
onInfoWindowClick = {
    scope.launch {
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(place.position, 12f),
            durationMs = 1000,
        )
    }
},
```

```kotlin
import androidx.compose.runtime.rememberCoroutineScope
import com.google.android.gms.maps.CameraUpdateFactory
import kotlinx.coroutines.launch
```

---

## Full example: map with markers

Everything from Part 2 together in `MainActivity.kt`:

```kotlin
package com.example.nacitmaps

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.nacitmaps.ui.theme.NacitMapsTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.launch

data class Place(val name: String, val description: String, val position: LatLng)

val places = listOf(
    Place("Lilongwe", "Capital city", LatLng(-13.9626, 33.7741)),
    Place("Blantyre", "Commercial hub", LatLng(-15.7861, 35.0058)),
    Place("Mzuzu", "Northern region", LatLng(-11.4656, 34.0207)),
    Place("Zomba", "Former capital", LatLng(-15.3850, 35.3188)),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NacitMapsTheme {
                Scaffold { paddingValues ->
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    val tappedPoints = remember { mutableStateListOf<LatLng>() }

                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(LatLng(-13.25, 34.3), 6f)
                    }

                    GoogleMap(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        cameraPositionState = cameraPositionState,
                        onMapLongClick = { latLng -> tappedPoints.add(latLng) },
                    ) {
                        // Fixed places, in blue.
                        places.forEach { place ->
                            Marker(
                                state = rememberUpdatedMarkerState(position = place.position),
                                title = place.name,
                                snippet = place.description,
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                                onInfoWindowClick = {
                                    Toast.makeText(context, "Zooming to ${place.name}", Toast.LENGTH_SHORT).show()
                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(place.position, 12f),
                                            durationMs = 1000,
                                        )
                                    }
                                },
                            )
                        }

                        // Pins the user dropped with a long press, in default red.
                        tappedPoints.forEach { point ->
                            Marker(
                                state = rememberUpdatedMarkerState(position = point),
                                title = "Dropped pin",
                                snippet = "%.4f, %.4f".format(point.latitude, point.longitude),
                            )
                        }
                    }
                }
            }
        }
    }
}
```

---

## Where to go next

- `maps-compose-utils` (already in the version catalog) — marker **clustering** when you have hundreds of pins.
- `Polyline`, `Polygon`, `Circle` composables — draw routes and areas inside the same `GoogleMap { }` block.
- `MarkerComposable` — use any Compose UI as a custom marker icon.
- Docs: <https://developers.google.com/maps/documentation/android-sdk/maps-compose>
