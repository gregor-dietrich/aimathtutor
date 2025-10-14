# TODO - Detailed Implementation Plans

## 1. Problem Completion Detection & AI Verification

**Goal:** Detect when a student reaches the target/solution expression and have the AI tutor provide congratulatory feedback.

**Implementation Plan:**

### Backend Changes

1. **GraspableEventDto** - Add field:
   - `boolean isComplete` - flag to indicate if this action resulted in completion

2. **GraspableMathService** - Add method:
   - `boolean checkCompletion(String currentExpression, String targetExpression)`
   - Parse and normalize both expressions (handle whitespace, order of terms)
   - Compare for mathematical equivalence (e.g., "x=5" == "5=x", "2x+3x" == "5x")
   - Return true if expressions are equivalent

3. **AITutorService** - Enhance `analyzeMathAction()`:
   - Check if `graspableEventDto.isComplete == true`
   - Generate special congratulatory feedback when complete
   - Examples: "🎉 Excellent work! You've solved it correctly!", "Perfect! You reached the solution x=5"

### Frontend Changes

1. **ExerciseWorkspaceView**:
   - In `onMathAction()`, call `graspableMathService.checkCompletion(expressionAfter, exercise.graspableTargetExpression)`
   - Set `event.isComplete` flag before sending to AI service
   - When complete, disable canvas interactions or show completion overlay
   - Display success notification

2. **GraspableMathView**:
   - For generated problems, store `targetExpression` when problem is loaded
   - Similar completion check as ExerciseWorkspaceView
   - Add optional "Next Problem" or "Try Another" button on completion

### Database Changes

- **StudentSessionEntity** - Update on completion:
  - Set `completed = true`
  - Set `endTime = LocalDateTime.now()`
  - Set `finalExpression = currentExpression`

---

## 2. Problem Category Selection for Generation

**Goal:** Allow users to choose from different math problem categories instead of always generating linear equations.

**Implementation Plan:**

### Backend Changes

1. **Create enum:** `ProblemCategory.java`

   ```java
   public enum ProblemCategory {
       LINEAR_EQUATIONS("Linear Equations", "algebra"),
       QUADRATIC_EQUATIONS("Quadratic Equations", "algebra"),
       POLYNOMIAL_SIMPLIFICATION("Polynomial Simplification", "algebra"),
       FACTORING("Factoring", "algebra"),
       FRACTIONS("Fraction Operations", "arithmetic"),
       EXPONENTS("Exponent Rules", "algebra"),
       SYSTEMS_OF_EQUATIONS("Systems of Equations", "algebra"),
       INEQUALITIES("Inequalities", "algebra")
   }
   ```

2. **AITutorService** - Enhance `generateProblem()`:
   - Change signature: `generateProblem(String difficulty, ProblemCategory category)`
   - Update AI prompt to include category-specific instructions
   - Return appropriate initial/target expressions for each category

3. **GraspableProblemDto** - Add field:
   - `ProblemCategory category`

### Frontend Changes

1. **GraspableMathView** - Replace "Generate New Problem" button:
   - Change to `ComboBox<ProblemCategory> categorySelect`
   - Add "Generate" button next to dropdown
   - Store selected category
   - Pass category to `aiTutorService.generateProblem()`

2. **UI Layout:**

   ```text
   [Category: Linear Equations ▼] [Generate Problem]
   ```

3. **Styling:**
   - Make category dropdown prominent
   - Default to LINEAR_EQUATIONS
   - Save last selected category in session/local storage (optional)

---

## 3. Multiple Problems Per Exercise with Sequential Unlocking

**Goal:** Allow exercises to have multiple problems (like hints), unlock "Next Problem" button when current is complete.

**Implementation Plan:**

### Backend Changes

1. **ExerciseEntity/ExerciseViewDto** - Modify fields:
   - `graspableInitialExpression` → Keep as is (semicolon-separated: "2x+5=15;3x-7=20;x^2=9")
   - `graspableTargetExpression` → Add this field (semicolon-separated: "x=5;x=9;x=3")
   - Parse expressions by splitting on `;` or `|`

2. **GraspableMathService** - Add session tracking:
   - `StudentSessionEntity.currentProblemIndex` (new field, default 0)
   - Track which problem in the sequence student is working on
   - Method: `int getCurrentProblemIndex(String sessionId)`
   - Method: `void advanceToNextProblem(String sessionId)`

3. **Database Migration:**
   - Add `current_problem_index INT DEFAULT 0` to `student_sessions` table
   - Add `graspable_target_expression VARCHAR(1000)` to `exercises` table

### Frontend Changes

1. **ExerciseWorkspaceView** - Add UI components:
   - Field: `int currentProblemIndex = 0`
   - Field: `String[] problems` (parsed from `exercise.graspableInitialExpression`)
   - Field: `String[] targetExpressions` (parsed from `exercise.graspableTargetExpression`)
   - Button: `nextProblemButton` (initially disabled)

2. **Problem Navigation:**
   - Load problem at index `currentProblemIndex` initially
   - When `checkCompletion()` returns true:
     - Enable `nextProblemButton` if `currentProblemIndex < problems.length - 1`
     - Disable canvas interactions until next problem loaded
   - On "Next Problem" click:
     - Increment `currentProblemIndex`
     - Call `graspableMathService.advanceToNextProblem(sessionId)`
     - Load next problem expression
     - Disable button again, re-enable canvas
   - Display progress: "Problem 2 of 3" in hints section

3. **Completion State:**
   - When last problem is completed, show final success message
   - Mark entire session as complete in database
   - Show "Back to Exercises" or "Review Session" options

### Admin/Teacher View

- Exercise creation form: Add help text explaining semicolon-separated format
- Example: "2x+5=15;3x-7=20" → Two problems in sequence

---

## 4. Admin Views for Progress Tracking

**Goal:** Create admin-only views to monitor student sessions, AI interactions, and overall progress.

**Implementation Plan:**

### Backend Changes

1. **New Service:** `AnalyticsService.java` (@ApplicationScoped)
   - `List<StudentSessionViewDto> getAllSessions()`
   - `List<StudentSessionViewDto> getSessionsByUser(Long userId)`
   - `List<StudentSessionViewDto> getSessionsByExercise(Long exerciseId)`
   - `List<AIInteractionViewDto> getAIInteractionsBySession(String sessionId)`
   - `StudentProgressSummaryDto getUserProgressSummary(Long userId)`
   - `Map<String, Integer> getProblemCategoryStats()` (how many problems solved per category)

2. **New DTOs:**
   - `StudentSessionViewDto` (expand existing with user/exercise names)
   - `AIInteractionViewDto` (event type, feedback given, timestamp)
   - `StudentProgressSummaryDto`:
     - `Long userId`, `String username`
     - `int totalSessions`, `int completedSessions`
     - `int totalProblems`, `int completedProblems`
     - `int hintsUsed`, `int averageActionsPerProblem`
     - `LocalDateTime lastActivity`

3. **Entity Enhancement:**
   - Ensure `AIInteractionEntity` has all needed fields:
     - `sessionId`, `eventType`, `feedbackMessage`, `timestamp`

### Frontend Changes

1. **New View:** `AdminDashboardView.java` (@Route "admin/dashboard")
   - Check user rank permissions (`rank.adminView == true`)
   - Display overview cards:
     - Total sessions today/week/month
     - Active students
     - Most attempted exercises
   - Charts/graphs (use Vaadin Charts if available, or simple tables)

2. **New View:** `StudentSessionsView.java` (@Route "admin/sessions")
   - Grid displaying all sessions with filters:
     - Columns: Student, Exercise, Start Time, Duration, Completed, Hints Used, Actions
     - Filter by: Student (dropdown), Exercise (dropdown), Date range, Completion status
   - Click row → Navigate to detailed session view

3. **New View:** `SessionDetailView.java` (@Route "admin/session/:sessionId")
   - Display complete session timeline:
     - Each action taken (expression before/after)
     - AI feedback given for each action
     - Hints revealed
     - Time spent on each step
   - Reconstruct the student's problem-solving path
   - Show final outcome (completed/abandoned)

4. **New View:** `StudentProgressView.java` (@Route "admin/progress")
   - Grid of all students with summary statistics
   - Columns: Username, Sessions, Completed, Success Rate, Last Activity
   - Click row → Detailed student profile with:
     - Session history
     - Strengths/weaknesses analysis (based on problem categories)
     - Time trends (improving/struggling)

5. **Navigation:**
   - Add "Admin" tab to MainLayout navigation bar (visible only if `rank.adminView == true`)
   - Submenu: Dashboard, Sessions, Student Progress

### Security

- Add checks in `beforeEnter()` for all admin views:

  ```java
  if (!authService.hasAdminView()) {
      event.rerouteTo(HomeView.class);
      NotificationUtil.showError("Access denied");
  }
  ```

---

## 5. User Settings Panel (Password & Avatar Selection)

**Goal:** Allow users to change their password and customize chat avatars (emojis).

**Implementation Plan:**

### Backend Changes

1. **UserEntity** - Add fields:
   - `String userAvatarEmoji` (default: "🧒")
   - `String tutorAvatarEmoji` (default: "🧑‍🏫")

2. **New DTO:** `UserSettingsDto.java`
   - `String currentPassword` (for verification)
   - `String newPassword`
   - `String userAvatarEmoji`
   - `String tutorAvatarEmoji`

3. **UserService** - Add methods:
   - `void changePassword(Long userId, String currentPassword, String newPassword)`
     - Verify current password matches
     - Hash new password with same salt
     - Update entity
   - `void updateAvatars(Long userId, String userEmoji, String tutorEmoji)`
     - Validate emojis (not empty, max length 10)
     - Update entity
   - `UserSettingsDto getSettings(Long userId)`

4. **Database Migration:**
   - Add columns to `users` table:
     - `user_avatar_emoji VARCHAR(10) DEFAULT '🧒'`
     - `tutor_avatar_emoji VARCHAR(10) DEFAULT '🧑‍🏫'`

### Frontend Changes

1. **New View:** `UserSettingsView.java` (@Route "settings")
   - Accessible from user menu in MainLayout
   - Tab layout with sections:

2. **Password Section:**
   - `PasswordField currentPassword` (label: "Current Password")
   - `PasswordField newPassword` (label: "New Password")
   - `PasswordField confirmPassword` (label: "Confirm New Password")
   - `Button changePassword` (primary)
   - Validation:
     - All fields required
     - New password must be at least 8 characters
     - New password must match confirm password
     - Show error if current password incorrect

3. **Avatar Section:**
   - Description: "Select emojis to represent yourself and the AI tutor in chat conversations"
   - `ComboBox<String> userAvatarSelect` (label: "Your Avatar")
     - Options: 🧒, 👦, 👧, 🧑, 👨, 👩, 🙂, 😊, 🤓, 🧠, ✏️, 📚
   - `ComboBox<String> tutorAvatarSelect` (label: "AI Tutor Avatar")
     - Options: 🧑‍🏫, 👨‍🏫, 👩‍🏫, 🤖, 🦉, 📖, 🎓, 💡, ⭐
   - Preview box showing sample messages with selected avatars
   - `Button saveAvatars` (primary)

4. **Integration with AIChatPanel:**
   - Modify `AIChatPanel.addMessage()` to use dynamic avatars:
     - Fetch user's avatar settings from `authService.getCurrentUser()`
     - Use `userAvatarEmoji` for USER messages
     - Use `tutorAvatarEmoji` for AI messages
     - Keep ℹ️ for SYSTEM messages

5. **UI Layout:**

   ```text
   Settings
   ├─ Password
   │  ├─ Current Password: [________]
   │  ├─ New Password: [________]
   │  ├─ Confirm Password: [________]
   │  └─ [Change Password]
   │
   └─ Chat Avatars
      ├─ Your Avatar: [🧒 ▼]
      ├─ AI Tutor Avatar: [🧑‍🏫 ▼]
      ├─ Preview:
      │  🧒 Hello, can you help me?
      │  🧑‍🏫 Of course! I'm here to help.
      └─ [Save Avatars]
   ```

6. **Restrictions:**
   - Do NOT allow changing: username, email, rank
   - Display current username/email as read-only fields for reference
   - Show message: "Contact an administrator to change your username or email"

### Service Integration

- **AuthService** - Add method:
  - `UserViewDto getCurrentUserWithAvatars()` (include avatar fields)
  - Cache avatars in session to avoid repeated DB queries

---

## Implementation Priority

Suggested order (easiest to hardest):

1. **User Settings Panel** (standalone feature, good UI/UX improvement)
2. **Problem Completion Detection** (enhances existing functionality)
3. **Problem Category Selection** (extends generation feature)
4. **Multiple Problems Per Exercise** (moderate complexity, requires DB changes)
5. **Admin Views** (most complex, requires multiple new views and services)

---

## Testing Checklist (for each feature)

- [ ] Unit tests for service methods
- [ ] Integration tests for DB operations
- [ ] Manual UI testing in both views
- [ ] Edge cases (empty data, invalid input, etc.)
- [ ] Permission/security checks
- [ ] Performance with large datasets (admin views)
