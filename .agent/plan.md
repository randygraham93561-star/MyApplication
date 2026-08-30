# Project Plan

Referee Schedule application for managing youth soccer referee assignments, game-day coverage, qualifications, points, and resources. 
Required details to include in the brief:
1. Recommended Android architecture (MVVM, Clean Architecture, Jetpack Compose).
2. Recommended backend architecture (Firebase or REST API with PostgreSQL).
3. Database schema entities (Users, Profiles, Games, Assignments, etc.).
4. Authentication strategy (Firebase Auth or similar).
5. API structure.
6. Project/file structure.
7. Real-time sync explanation (WebSockets/Realtime DB).
8. Offline functionality (Room, sync queue).
9. Deployment independence explanation.

Features: Real-time Day of Scheduler, overlapping assignment prevention, qualification/comfort level warnings, referee point tracking, game check-in/reports, lunch vouchers, mentor system, laws of the game/education materials, and referee badge progression.

## Project Brief

# Project Brief: Referee Schedule App

This project brief outlines the development of a Minimum Viable Product (MVP) for the Referee Schedule application, designed to streamline youth soccer referee assignments and game-day management.

## Features
1.  **Real-time Game Scheduling & Assignments**: A centralized dashboard for administrators to post games and for referees to view and accept assignments in real-time.
2.  **Assignment Validation & Warnings**: Automated prevention of overlapping assignments and system warnings for qualification or "comfort level" mismatches (e.g., assigning a junior referee to a high-intensity U19 match).
3.  **Game-Day Operations (Check-in & Reporting)**: Digital check-in for referees, match report submission, and lunch voucher distribution directly through the app.
4.  **Referee Point & Badge Progression**: A tracking system for referee points earned per match and a visual progress tracker for badge level upgrades.
5.  **Resource & Mentor Hub**: Access to the Laws of the Game, educational materials, and a request system for mentor support during match days.

## High-Level Technical Stack
*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Material 3)
*   **Navigation**: **Jetpack Navigation 3** (State-driven)
*   **Adaptive Strategy**: **Compose Material Adaptive** library (supporting Phone, Foldable, and Tablet layouts)
*   **Asynchrony**: Kotlin Coroutines & Flow
*   **Dependency Injection**: Hilt

---

## Architectural Recommendations

### 1. Android Architecture
*   **MVVM (Model-View-ViewModel)**: Used to separate UI logic from business logic, ensuring a reactive UI state.
*   **Clean Architecture**: Separation into **Data** (Repository, API, DB), **Domain** (Use Cases, Entities), and **UI** (ViewModels, Composables) layers for testability and maintainability.

### 2. Backend Architecture
*   **Recommendation**: **Firebase** for rapid MVP development.
    *   **Firestore**: NoSQL database for flexible game and assignment structures.
    *   **Cloud Functions**: To handle complex server-side logic like assignment validation and point calculations.
*   *Alternative*: REST API (Node.js/Ktor) with **PostgreSQL** if complex relational queries (e.g., advanced reporting) become the primary bottleneck.

### 3. Database Schema (Entities)
*   **Users**: Auth ID, Email, Role (Referee, Admin, Mentor).
*   **Profiles**: Name, Badge Level, Comfort Level, Total Points, Qualifications.
*   **Games**: Date, Time, Location, Field Number, Age Group, Required Crew Size.
*   **Assignments**: Game ID, Referee ID, Position (Center, AR1, AR2), Status (Pending, Confirmed).
*   **Reports**: Game ID, Score, Disciplinary Actions, Referee Notes.

### 4. Authentication Strategy
*   **Firebase Auth**: Supports Email/Password and Google Sign-in for ease of access. 
*   **Role-Based Access Control (RBAC)**: Custom claims in Firebase tokens to distinguish between Referees and Assignors.

### 5. API Structure
*   **Firebase SDK**: Direct integration for real-time data flow.
*   **Structured Endpoints** (if using REST):
    *   `GET /games/available`
    *   `POST /assignments/claim`
    *   `POST /reports/submit`

### 6. Project/File Structure
```text
com.example.refereeapp/
├── data/
│   ├── repository/
│   ├── local/ (Room/Preferences)
│   └── remote/ (Firebase/Retrofit)
├── domain/
│   ├── model/
│   └── usecase/
├── ui/
│   ├── components/ (Common UI)
│   ├── navigation/ (Nav 3 configuration)
│   └── features/ (e.g., scheduler, profile, reports)
└── di/ (Hilt Modules)
```

### 7. Real-time Sync
*   **Mechanism**: **Firebase Realtime Database or Firestore Snapshots**. 
*   **Usage**: Ensures that when a game is claimed, it immediately disappears or updates for all other users to prevent double-booking.

### 8. Offline Functionality
*   **Room Database**: Local caching of the referee's personal schedule and the Laws of the Game.
*   **Sync Queue**: Match reports submitted without signal are stored in Room and pushed to the backend using **WorkManager** once connectivity is restored.

### 9. Deployment Independence
The app is designed with a **Repository Pattern**. This allows the frontend to remain independent of the specific backend implementation. If the project migrates from Firebase to a custom REST API in the future, only the `Data` layer needs modification; the `Domain` and `UI` layers remain untouched.

## Implementation Steps
**Total Duration:** 1h 27m 18s

### Task_1_Foundation_Auth: Initialize the Android project with Hilt, Jetpack Navigation 3, and Firebase. Implement the Authentication flow including Sign-in and Sign-up using Firebase Auth.
- **Status:** COMPLETED
- **Updates:** Completed the foundation setup for the Referee Schedule app.
- **Acceptance Criteria:**
  - Hilt dependency injection is set up
  - Jetpack Navigation 3 is configured as the app's navigation engine
  - Firebase Auth is integrated with Email/Password support
  - google-services.json and API keys are correctly integrated
  - App launches to a login screen and navigates to a placeholder dashboard upon success
- **Duration:** 42m 39s

### Task_2_Data_Domain_Layer: Define domain models and implement Firestore repositories for Users, Profiles, Seasons, Teams, Games, and Assignments. Ensure real-time synchronization is handled via Firestore snapshots.
- **Status:** COMPLETED
- **Updates:** Implemented the Data and Domain layers using Clean Architecture and the Repository pattern.
- **Acceptance Criteria:**
  - Repository pattern is implemented in the Data layer
  - Firestore schemas for Phase 1 entities are defined
  - Real-time sync for Game assignments is functional
  - Repository tests or basic integration verified
- **Duration:** 22m 21s

### Task_3_Referee_Scheduler_UI: Develop the Referee Dashboard and Profile screens using Jetpack Compose and Material 3. Implement the 'Day of Scheduler' featuring a real-time game list and the ability to claim assignments with automated overlap and qualification checking.
- **Status:** COMPLETED
- **Updates:** Successfully developed the Referee Scheduler and Profile UI.
- **Acceptance Criteria:**
  - Referee Profile management is functional
  - Game list updates in real-time when assignments are claimed
  - Overlap detection prevents double-booking a referee
  - Qualification check triggers warnings for mismatching badge/comfort levels
  - UI uses Compose Material Adaptive for phone and tablet layouts
- **Duration:** 10m 24s

### Task_4_GameOps_Reporting_Admin: Implement Game-Day operations including Digital Check-in, Match Report submission, and Point/Badge tracking. Add a basic Administrator Dashboard to view all games and referee status.
- **Status:** COMPLETED
- **Updates:** Implemented Game-Day operations, point tracking, and administrative controls.
- Added digital check-in functionality for referees.
- Developed the MatchReportScreen for score and disciplinary reporting.
- Implemented automatic point awarding (2 pts for Head Ref, 1 pt for Assistant Ref) and team point tracking.
- Created an AdminDashboardScreen with summary statistics and assignment management.
- Added logic for approving/denying pending assignment requests (for comfort level exceptions).
- Secured routes and UI elements based on User Roles (Referee vs. Admin).
- Verified build stability.
- **Acceptance Criteria:**
  - Referee can check-in to a game digitally
  - Match report form successfully saves data to Firestore
  - Referee points and badge progress update correctly in the profile
  - Admin dashboard displays a list of all games and their assignment status
- **Duration:** 11m 54s

### Task_5_Run_and_Verify: Perform a comprehensive final verification of the application. Ensure all Phase 1 and 2 features are stable, offline functionality basics are in place (Room caching), and the UI aligns with Material 3 standards.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Build passes and app does not crash
  - All existing tests pass
  - Critical UI issues reported by critic_agent are addressed
  - The implemented UI must match the design provided in project documentation
  - Application stability verified across Phone and Tablet configurations
- **StartTime:** 2026-08-26 12:54:28 PDT

