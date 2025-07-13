# Authentication System for QuantumLift

This document explains how to set up and use the authentication system in the QuantumLift Android app.

## 🚀 Quick Start

### 1. Start the Backend Server

```bash
cd server
npm run dev
```

The server will start on `http://localhost:3000`

### 2. Test the Server

You can test the authentication endpoints using PowerShell:

**Register a new user:**
```powershell
Invoke-WebRequest -Uri "http://localhost:3000/api/auth/register" -Method POST -ContentType "application/json" -Body '{"username":"testuser","email":"test@example.com","password":"password123"}'
```

**Login with existing user:**
```powershell
Invoke-WebRequest -Uri "http://localhost:3000/api/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"testuser","password":"password123"}'
```

## 📱 Android App Features

### Authentication Flow

1. **App Launch**: The app checks if the user is logged in
2. **Not Logged In**: Shows login screen
3. **Logged In**: Goes directly to the main app

### Screens

- **Login Screen**: Username/password login with modern UI
- **Register Screen**: Create new account with email validation
- **Settings Screen**: Logout functionality

### Key Features

- ✅ JWT token storage using DataStore
- ✅ Secure password handling
- ✅ Modern Material 3 UI design
- ✅ Error handling and loading states
- ✅ Automatic navigation based on auth state
- ✅ Logout functionality

## 🔧 Technical Details

### Dependencies Added

```kotlin
// Retrofit for API calls
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// JWT token handling
implementation("com.auth0.android:jwtdecode:2.0.1")

// DataStore for secure token storage
implementation("androidx.datastore:datastore-preferences:1.0.0")
```

### Permissions Added

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### API Endpoints

- **Base URL**: `http://10.0.2.2:3000/` (for Android emulator)
- **Register**: `POST /api/auth/register`
- **Login**: `POST /api/auth/login`

### Data Models

- `LoginRequest`: Username and password
- `RegisterRequest`: Username, email, and password
- `AuthResponse`: Server response with user data and token
- `User`: User information

## 🎨 UI Components

### Login Screen
- Gradient background with blue theme
- Card-based form design
- Password visibility toggle
- Error message display
- Loading states

### Register Screen
- Same design as login screen
- Additional email and confirm password fields
- Password confirmation validation
- Email format validation

## 🔐 Security Features

- JWT tokens stored securely in DataStore
- Password fields with visual transformation
- Network security configuration
- Error handling for network issues

## 🧪 Testing

### Server Testing
The server endpoints are working correctly:
- ✅ Registration endpoint returns proper responses
- ✅ Login endpoint returns JWT tokens
- ✅ Error handling for duplicate users

### Android Testing
To test the Android app:
1. Start the server: `npm run dev`
2. Build and run the Android app
3. Try registering a new user
4. Try logging in with existing credentials
5. Test logout functionality

## 🚨 Troubleshooting

### Common Issues

1. **Server not starting**: Make sure PostgreSQL is running
2. **Network errors**: Check if server is on port 3000
3. **Build errors**: Ensure Java 17 is installed for Android build

### Debug Tips

- Check server logs for API errors
- Use Android Studio's network inspector
- Verify DataStore is saving tokens correctly

## 📝 Notes

- The app uses `10.0.2.2` for Android emulator (localhost equivalent)
- For real devices, you'll need to use your PC's local IP address
- JWT tokens are automatically handled by the AuthRepository
- The authentication state is managed by AuthViewModel

## 🔄 Next Steps

1. Add password reset functionality
2. Implement email verification
3. Add biometric authentication
4. Create user profile management
5. Add social login options 