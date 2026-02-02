I have analyzed the requirements and checked the existing database structure.

**1. Activity Details Verification**
- **Database Structure**: I have verified the `Activity` entity (`Activity.java`). It **already contains** the necessary fields:
  - `location` (String) - for the location.
  - `description` (String, TEXT) - for the detail introduction.
  - `club` (ManyToOne relation) - for the organizer unit.
- **Conclusion**: The database structure is correct and does not need modification. The missing information in the user interface is due to the frontend not displaying these fields yet.

**2. Implementation Plan**
I will proceed with the following steps to complete the tasks:

### Step 1: Enhance Activity Details Display (Frontend)
- **Modify `MyActivitiesView.vue`**:
  - Add an "Organizer" (Club Name) column to the table.
  - Add a "View Details" action (or clickable title) that opens a Dialog showing the full `description`, `location`, `time`, and `coverUrl`.

### Step 2: Implement Station Letter / Notification System (Backend)
- **Create Entity**: `Notification` in `community-core` (fields: `userId`, `title`, `content`, `type`, `isRead`, `createdAt`).
- **Create Repository**: `NotificationRepository`.
- **Create Service**: `NotificationService` (send, get list, mark read).
- **Create Controller**: `NotificationController` with endpoints:
  - `GET /api/notifications`
  - `PUT /api/notifications/{id}/read`
  - `PUT /api/notifications/read-all`

### Step 3: Verification
- Verify the "My Activities" page shows the organizer and allows viewing details.
- Verify the "Notifications" page loads mock/real notifications and allows marking them as read.
