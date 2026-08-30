# Initialize Android Project with Hilt, Navigation 3, and Firebase

This plan covers the initialization of Hilt for dependency injection, Jetpack Navigation 3 for navigation, and Firebase Auth for user management.

## User Review Required

> [!IMPORTANT]
> Firebase requires a `google-services.json` file to be placed in the `app/` directory. Please provide this file or confirm if I should proceed with dummy configuration for now.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/gradle/libs.versions.toml)
- Add Hilt, Firebase, and Google Services dependencies and plugins.

#### [MODIFY] [build.gradle.kts (root)](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/build.gradle.kts)
- Apply Hilt and Google Services plugins.

#### [MODIFY] [build.gradle.kts (app)](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/build.gradle.kts)
- Apply Hilt and Google Services plugins.
- Add dependencies for Hilt and Firebase Auth.

---

### Application Setup

#### [NEW] [MyApplication.kt](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/example/myapplication/MyApplication.kt)
- Create Hilt-annotated Application class.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/AndroidManifest.xml)
- Register `MyApplication`.

---

### Authentication Layer

#### [NEW] [AuthRepository.kt](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/example/myapplication/data/AuthRepository.kt)
- Interface and implementation for Firebase Auth operations.

#### [NEW] [AuthModule.kt](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/example/myapplication/di/AuthModule.kt)
- Hilt module to provide `FirebaseAuth` and `AuthRepository`.

---

### Navigation 3 Setup

#### [NEW] [NavRoutes.kt](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/example/myapplication/navigation/NavRoutes.kt)
- Define serializable `NavKey` objects for Login, Sign-up, and Dashboard.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/example/myapplication/MainActivity.kt)
- Set up `NavBackStack` and `NavDisplay`.
- Inject ViewModels using Hilt.

---

### UI Screens

#### [NEW] [LoginScreen.kt](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/example/myapplication/ui/auth/LoginScreen.kt)
#### [NEW] [SignUpScreen.kt](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/example/myapplication/ui/auth/SignUpScreen.kt)
#### [NEW] [DashboardScreen.kt](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/example/myapplication/ui/dashboard/DashboardScreen.kt)

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to verify build.
- (Optional) Unit tests for `AuthRepository`.

### Manual Verification
- Launch the app and verify the start destination is Login.
- Navigate to Sign-up and back.
- Simulate a successful login (if possible without real Firebase setup) to see the Dashboard.
