# Implementation Plan - Game-Day Operations & Administrator Features

This plan outlines the steps to implement referee check-in, match reporting, points tracking, and an administrator dashboard.

## User Review Required

> [!IMPORTANT]
> The "Check-in" button will be enabled 30 minutes before the scheduled game time.
> Match reports can only be submitted by the assigned referees after the game has started.

## Proposed Changes

### Domain Models

#### [MODIFY] [Game.kt](file:///C:/Users/The%20Randall%20Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/domain/model/Game.kt)
- Add `Completed` to `GameStatus`.
- Add fields for scores, cards, notes, and report submission metadata.

#### [MODIFY] [Assignment.kt](file:///C:/Users/The%20Randall%20Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/domain/model/Assignment.kt)
- Add `checkedIn: Boolean` field.

### Repositories

#### [MODIFY] [AssignmentRepository.kt](file:///C:/Users/The%20Randall%20Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/domain/repository/AssignmentRepository.kt) & [AssignmentRepositoryImpl.kt](file:///C:/Users/The%20Randall%20Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/data/repository/AssignmentRepositoryImpl.kt)
- Add `checkIn` method.

#### [MODIFY] [GameRepository.kt](file:///C:/Users/The%20Randall%20Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/domain/repository/GameRepository.kt) & [GameRepositoryImpl.kt](file:///C:/Users/The%20Randall%20Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/data/repository/GameRepositoryImpl.kt)
- Add `submitReport` method.

#### [MODIFY] [ProfileRepository.kt](file:///C:/Users/The%20Randall%20Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/domain/repository/ProfileRepository.kt) & [ProfileRepositoryImpl.kt](file:///C:/Users/The%20Randall%20Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/data/repository/ProfileRepositoryImpl.kt)
- Add logic to update points.

### Navigation

#### [MODIFY] [NavRoutes.kt](file:///C:/Users/The%20Randall%20Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/navigation/NavRoutes.kt)
- Add `MatchReport(val gameId: String)` and `AdminDashboard`.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/The%20Randall%20Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/MainActivity.kt)
- Wire up the new routes.

### UI Screens

#### [MODIFY] [DashboardScreen.kt](file:///C:/Users/The%20Randall%20Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/ui/dashboard/DashboardScreen.kt)
- Display upcoming assigned games.
- Add Check-in and Report buttons.
- Add navigation to Admin Dashboard for admins.

#### [NEW] [MatchReportScreen.kt](file:///C:/Users/The%20Randall%20Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/ui/report/MatchReportScreen.kt) & [MatchReportViewModel.kt](file:///C:/Users/The%20Randall%20Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/ui/report/MatchReportViewModel.kt)
- Form to enter match results and notes.
- Logic to award points and update game status.

#### [NEW] [AdminDashboardScreen.kt](file:///C:/Users/The%20Randall%20Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/ui/admin/AdminDashboardScreen.kt) & [AdminDashboardViewModel.kt](file:///C:/Users/The%20Randall%20Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/ui/admin/AdminDashboardViewModel.kt)
- Stats summary.
- Management of games and pending assignments.

## Verification Plan

### Automated Tests
- `./gradlew test` to verify point awarding logic.

### Manual Verification
- Log in as a referee, check in to a game, and submit a report.
- Verify points are updated in the profile.
- Log in as an admin and access the admin dashboard.
