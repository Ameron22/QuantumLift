# BUILD ERROR DIARY - QuantumLift Project

## Logbook Format - Chronological History

### Entry 1: Java Version Mismatch (2024-12-19)
- **Problem**: Build failing due to Java version incompatibility
- **Solution**: Located Java 17 and updated gradle.properties
- **Status**: ✅ RESOLVED

### Entry 2: Kapt vs KSP Plugin Conflicts (2024-12-19)
- **Problem**: kapt plugin conflicting with Retrofit interfaces
- **Solution**: Migrated from kapt to KSP (Kotlin Symbol Processing)
- **Status**: ✅ RESOLVED

### Entry 3: Plugin Version Mismatches (2024-12-19)
- **Problem**: KSP plugin not found due to version catalog issues
- **Solution**: Updated plugin declarations in version catalog and build files
- **Status**: ✅ RESOLVED

### Entry 4: Compose Plugin References (2024-12-19)
- **Problem**: Unresolved references to Compose plugin in build.gradle.kts
- **Solution**: Removed plugin alias and cleaned caches
- **Status**: ✅ RESOLVED

### Entry 5: Room Entity Field Type (2024-12-19)
- **Problem**: `parts` field in EntityExercise was List<String> which Room cannot handle
- **Solution**: Changed to String storing JSON, added Converter class
- **Status**: ✅ RESOLVED

### Entry 6: Persistent Build Cache Issues (2024-12-19)
- **Problem**: Stale generated files causing "NonExistentClass cannot be converted to Annotation"
- **Solution**: Extensive cache clearing and build directory removal
- **Status**: ✅ RESOLVED

### Entry 7: Gradle Cache Corruption (2024-12-19)
- **Problem**: `C:\Users\coolm\.gradle\caches\8.11.1\groovy-dsl\2d27ca9a89f708695592b2c3aed807c2\metadata.bin (The system cannot find the path specified)`
- **Solution**: 
  - Killed all Java/Gradle processes
  - Completely removed global Gradle cache: `C:\Users\coolm\.gradle`
  - Cleared project Gradle cache: `.gradle` directory
  - Stopped Gradle daemon: `.\gradlew --stop`
  - Used `--no-daemon` flag for fresh builds
- **Status**: ✅ RESOLVED

### Entry 8: KSP/Room [MissingType] Error (2024-12-19)
- **Problem**: `[MissingType]: Element 'com.example.gymtracker.data.EntityExercise' references a type that is not present`
- **Solution**: Added `id("kotlin-parcelize")` to the plugins block in `app/build.gradle.kts`
- **Status**: ✅ RESOLVED

### Entry 9: Compose Compiler/Kotlin Version Mismatch (2024-12-19)
- **Problem**: `This version (1.3.2) of the Compose Compiler requires Kotlin version 1.7.20 but you appear to be using Kotlin version 1.9.22`
- **Solution**: 
  - Updated Compose BOM version to "2024.10.00"
  - Added `composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }` to android block
- **Status**: ✅ RESOLVED

### Entry 10: Missing Dependencies (Coil, GIF, Material Icons) (2024-12-19)
- **Problem**: Unresolved references to `coil`, `pl.droidsonroids.gif.GifImageView`, `Visibility`, `VisibilityOff`
- **Solution**: 
  - Added Coil dependencies: `coil`, `coil.compose`, `coil.gif`
  - Added GIF library: `pl.droidsonroids.gif:android-gif-drawable`
  - Added Material Icons: `androidx.compose.material:material-icons-extended`
  - Fixed imports in LoginScreen and RegisterScreen for `Icons.Filled.Visibility` and `Icons.Filled.VisibilityOff`
- **Status**: ✅ RESOLVED

### Entry 11: Type Mismatches and Code Errors (2024-12-19)
- **Problem**: 
  - Type mismatches and inference errors in `AddExerciseToWorkoutScreen.kt`, `MainActivity.kt`, `SettingsScreen.kt`
  - @Composable invocation errors in `SettingsScreen.kt`
  - Unresolved reference errors in `ExerciseScreen.kt` (joinToString issue)
- **Solution**:
  - Fixed `exercise.parts` usage by using `Converter().fromString()` to convert JSON string back to List<String>
  - Fixed @Composable invocation error in SettingsScreen by removing `viewModel<AuthViewModel>` from Button onClick
  - Fixed `onNewIntent` method signature in MainActivity
  - Fixed AuthViewModel factory type mismatch using proper Compose ViewModelProvider.Factory pattern
- **Status**: ✅ RESOLVED

---

## Key Lessons Learned
- When Gradle cache corruption occurs, completely removing the `.gradle` directory is the most effective solution
- Using `--no-daemon` flag helps avoid cached daemon issues
- Always kill Java/Gradle processes before clearing caches
- Material Icons require proper imports: `Icons.Filled.Visibility` not `Icons.Default.Visibility`
- Compose compiler version must match Kotlin version
- Room entities with complex types need proper TypeConverters
- App name is "QuantumLift" not "GymTracker"

## Current Status
- ✅ All build configuration issues resolved
- ✅ Database entities properly configured
- ✅ Gradle cache corruption fixed
- ✅ Dependencies (Coil, GIF, Material Icons) added and working
- ✅ Code-level type mismatches and logic errors fixed
- ✅ Network security configuration added for development
- ✅ Text color issues fixed in login/register screens
- ✅ Comprehensive logging added for authentication debugging
- 🔄 Ready for final build test

### Entry 12: Network Security and UI Issues (2024-12-19)
- **Problem**: 
  - Cleartext communication to 10.0.2.2 blocked by network security policy
  - White text in input fields not readable on white background
  - No logging for authentication debugging
- **Solution**:
  - Created `network_security_config.xml` to allow cleartext traffic for development domains
  - Added `android:networkSecurityConfig="@xml/network_security_config"` and `android:usesCleartextTraffic="true"` to AndroidManifest.xml
  - Fixed text color in LoginScreen and RegisterScreen by using `textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black)` and proper Material3 color parameters
  - Added comprehensive logging with "AUTH_LOG" and "AUTH_REPO" tags in AuthViewModel and AuthRepository for debugging
- **Status**: ✅ RESOLVED

### Entry 13: Material3 OutlinedTextField Color Parameters (2024-12-19)
- **Problem**: 
  - Compilation errors due to incorrect parameter names in `OutlinedTextFieldDefaults.colors()`
  - Parameters like `textColor`, `focusedBorderColor`, etc. not found in Material3
- **Solution**:
  - Replaced `textColor` parameter with `textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black)`
  - Used correct Material3 parameters: `focusedContainerColor`, `unfocusedContainerColor`, `cursorColor`
  - Added explicit color to label Text components: `Text("Label", color = Color.Black)`
- **Status**: ✅ RESOLVED

### Entry 14: Android Emulator Network Connection Issue (2024-12-19)
- **Problem**: 
  - Android app unable to connect to server at `10.0.2.2:3000`
  - SocketTimeoutException after 10 seconds
  - Server running on localhost:3000 but emulator can't reach it
- **Solution**:
  - Changed server URL from `10.0.2.2:3000` to `192.168.0.76:3000` (computer's actual IP)
  - Updated `network_security_config.xml` to allow the new IP address
  - Added enhanced logging to track connection attempts and server URL
  - Verified server is running and accessible on the network
- **Status**: ✅ RESOLVED

### Entry 15: Railway Deployment Preparation (2024-12-19)
- **Problem**: 
  - Need to deploy app to production for external access
  - Local development only works on same WiFi network
  - Mobile data users can't access local server
- **Solution**:
  - Updated `database.js` to support Railway's `DATABASE_URL`
  - Added SSL configuration for production PostgreSQL
  - Created `railway.json` configuration file
  - Updated `AuthRepository.kt` to support both development and production URLs
  - Created comprehensive deployment guide with step-by-step instructions
  - Prepared environment variables for production deployment
- **Status**: ✅ READY FOR DEPLOYMENT

### Entry 16: Vercel Free Forever Deployment (2024-12-19)
- **Problem**: 
  - Railway now has 30-day trial, then $5/month
  - Need free forever solution for personal project
  - Want to keep costs at $0 for personal use
- **Solution**:
  - Switched to Vercel (free forever) for hosting
  - Created `vercel.json` configuration file
  - Recommended PlanetScale (free forever) for database
  - Created comprehensive Vercel deployment guide
  - Updated database configuration to support external databases
  - Provided multiple free database options (PlanetScale, Supabase, Neon)
- **Status**: ✅ FREE FOREVER SOLUTION READY