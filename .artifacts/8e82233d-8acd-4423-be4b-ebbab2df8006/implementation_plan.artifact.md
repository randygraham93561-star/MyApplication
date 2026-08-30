# Multi-Organization Support and Admin Profile Expansion

This plan introduces multi-tenancy to the Referee Schedule app, allowing a "System Admin" to create and manage multiple organizations, and "Referee Admins" to manage their own organization's referees and games in isolation.

## User Review Required

> [!IMPORTANT]
> This change impacts the data schema of almost every domain entity. Existing data will need migration or will be treated as belonging to a "default" organization.

> [!NOTE]
> We will add a new `SystemAdmin` role. The existing `Admin` role will be repurposed as `OrganizationAdmin` (Referee Admin).

## Proposed Changes

### Domain Models
Update models to support organization-based data isolation.

#### [NEW] [Organization.kt](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/domain/model/Organization.kt)
Create a new model for soccer organizations.
```kotlin
data class Organization(
    val id: String = "",
    val name: String = "",
    val contactEmail: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
```

#### [MODIFY] [User.kt](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/domain/model/User.kt)
* Add `organizationId: String?`.
* Add `SystemAdmin` to `UserRole`.

#### [MODIFY] [Entities](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/domain/model/)
Add `organizationId: String` to:
* `RefereeProfile`
* `Game`
* `Team`
* `Season`
* `Assignment`

---

### Repositories
Update repositories to filter data by the user's `organizationId`.

#### [MODIFY] [Repositories](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/domain/repository/)
Update interface methods to accept `organizationId` or use a session-based approach.

---

### UI / Presentation
Introduce new screens for organization management and update existing admin screens.

#### [NEW] [SystemAdminDashboard](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/ui/admin/system/SystemAdminDashboard.kt)
A screen for the System Admin to:
* List all organizations.
* Create new organizations.
* Create/Assign Referee Admins to organizations.

#### [MODIFY] [AdminDashboardScreen](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/ui/admin/AdminDashboardScreen.kt)
Update the Referee Admin dashboard to display organization-specific information.

#### [NEW] [AdminProfileScreen](file:///C:/Users/The Randall Truck/AndroidStudioProjects/MyApplication/app/src/main/java/com/google/refereeschedule/ui/admin/profile/AdminProfileScreen.kt)
A specific profile view for Referee Admins to manage their organization's settings.

## Verification Plan

### Automated Tests
* Unit tests for repository filtering: Ensure `GameRepository` only returns games for the specified `organizationId`.
* Integration tests for role-based navigation: Ensure `SystemAdmin` can access organization management, while `RefereeAdmin` cannot.

### Manual Verification
1. Log in as a `SystemAdmin`.
2. Create two organizations: "League A" and "League B".
3. Create a `RefereeAdmin` for "League A".
4. Log in as the "League A" admin and create a game.
5. Verify that the game is NOT visible when logged in as a "League B" admin or referee.
