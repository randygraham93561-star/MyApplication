# Referee Schedule App - Referee UI and Scheduler Features

Implementation of the Referee Profile, Scheduler Screen, and Claim Assignment Flow.

## Proposed Changes

### Domain & Data Layer

#### [MODIFY] [RefereeProfile.kt](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/domain/model/RefereeProfile.kt)
- Add `phoneNumber` field.
- Ensure all required fields for profile editing are present.

#### [MODIFY] [Game.kt](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/domain/model/Game.kt)
- Add `homeTeam` and `awayTeam` fields (or just a string list for teams).
- The prompt says "teams", so I'll add `homeTeamName` and `awayTeamName`.

### UI Layer

#### [NEW] [ProfileScreen.kt](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/ui/profile/ProfileScreen.kt)
- Referee profile view/edit screen.
- Fields: name, phone, comfort levels (Head vs Assistant), team association.
- Read-only fields: badge level, points.

#### [NEW] [SchedulerScreen.kt](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/ui/scheduler/SchedulerScreen.kt)
- Real-time list of today's games.
- Visual status indicators (Green/Yellow/Red).
- Uses `ListDetailPaneScaffold` for adaptive layout.

#### [NEW] [ClaimAssignmentDialog.kt](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/ui/scheduler/ClaimAssignmentDialog.kt)
- Validation logic for overlap and comfort level.
- Warning/Confirmation for +1 level mismatch.
- "Request Admin Approval" for +2 level mismatch.

### Navigation

#### [MODIFY] [NavRoutes.kt](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/navigation/NavRoutes.kt)
- Add `Profile` and `Scheduler` routes.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/MainActivity.kt)
- Integrate new screens into `NavDisplay`.
- Add a bottom navigation or drawer to switch between Dashboard, Scheduler, and Profile.

## Verification Plan

### Automated Tests
- Unit tests for overlap detection logic.
- Unit tests for comfort level validation logic.

### Manual Verification
- Verify real-time updates of game status.
- Test double-booking prevention.
- Test adaptive layout on different screen sizes (simulated).
