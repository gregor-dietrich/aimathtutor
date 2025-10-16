# TODO - Detailed Implementation Plans

## 0. General

### Implementation Priority

### Suggested Order (Easiest to Hardest)

1. **Multiple Problems Per Exercise** (Task 2)
   *Moderate-Complex*: Involves DB changes, session tracking, and sequential UI logic.

2. **AdminConfigView: Runtime AI Provider/Model/Settings Management** (Task 5)
   *Complex*: Requires dynamic config management, secure runtime updates, and advanced UI/UX for admin settings.

3. **Admin Views for Progress Tracking** (Task 3)
   *Most Complex*: Multiple new views, analytics, charts, security checks, and extensive backend/frontend integration.

**Difficulty Ratings:**

- Task 2: ★★★☆☆
- Task 5: ★★★★☆
- Task 3: ★★★★★

### Testing Checklist (for each feature)

- [ ] Unit tests for service methods
- [ ] Integration tests for DB operations
- [ ] Manual UI testing in both views
- [ ] Edge cases (empty data, invalid input, etc.)
- [ ] Permission/security checks
- [ ] Performance with large datasets (admin views)

---

## 2. Multiple Problems Per Exercise with Sequential Unlocking

**Goal:** Allow exercises to have multiple problems (like hints), unlock "Next Problem" button when current is complete.

**Implementation Plan:**

### 2.1 Backend Changes

1. **ExerciseEntity/ExerciseViewDto** - Modify fields:
   - `graspableInitialExpression` → Keep as is (semicolon-separated: "2x+5=15;3x-7=20;x^2=9")
   - `graspableTargetExpression` → Add this field (semicolon-separated: "x=5;x=9;x=3")
   - Parse expressions by splitting on `;` or `|`

2. **GraspableMathService** - Add session tracking:
   - `StudentSessionEntity.currentProblemIndex` (new field, default 0)
   - Track which problem in the sequence student is working on
   - Method: `int getCurrentProblemIndex(String sessionId)`
   - Method: `void advanceToNextProblem(String sessionId)`

3. **Database Changes:**
   - Add `current_problem_index INT DEFAULT 0` to `student_sessions` table
   - Add `graspable_target_expression VARCHAR(1000)` to `exercises` table
   - Add them to the existing init script - do NOT create separate scripts

### 2.2 Frontend Changes

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

4. **Admin/Teacher View**
   - Exercise creation form: Add help text explaining semicolon-separated format
   - Example: "2x+5=15;3x-7=20" → Two problems in sequence

---

## 3. Admin Views for Progress Tracking

**Goal:** Create admin-only views to monitor student sessions, AI interactions, and overall progress.

**Implementation Plan:**

### 3.1 Backend Changes

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

### 3.2 Frontend Changes

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

### 3.3 Security

- Add checks in `beforeEnter()` for all admin views:

  ```java
  if (!authService.hasAdminView()) {
      event.rerouteTo(HomeView.class);
      NotificationUtil.showError("Access denied");
  }
  ```

---

## 5. AdminConfigView: Runtime AI Provider/Model/Settings Management

**Goal:** Transform the admin home view into `AdminConfigView`, allowing users with admin privileges to change application-wide AI settings at runtime.

**Implementation Plan:**

### 5.1 Backend Changes

1. **Config Properties:**
   - Rename `ai.tutor.provider` → `ai.tutor.default.provider`
   - Rename `gemini.model` → `gemini.default.model`
   - Rename `openai.model` → `openai.default.model`
   - Rename `ollama.model` → `ollama.default.model`
   - Add unified properties for `ai.tutor.max-tokens` and `ai.tutor.temperature` (not per-provider)
   - Add `openai.organization-id`, `ollama.timeout-seconds`, and provider-specific API URLs

2. **Service:**
   - Add service for updating config properties at runtime (with validation and security checks)

### 5.2 Frontend Changes

1. **AdminConfigView:**
   - Replace admin home view with a config panel for AI settings
   - Dropdowns for AI provider and model (model dropdown updates when provider changes)
   - Disable Gemini/OpenAI if API key is unset ("your-api-key-here"), disable Ollama if URL is unset ("your-ollama-api-url-here")
   - Inputs for max-tokens and temperature (always visible, affect selected provider)
   - Inputs for OpenAI organization ID and Ollama timeout (hidden unless respective provider selected, ideally with smooth animation)
   - Input for API URL (affects only selected provider)

2. **UI/UX:**
   - Show/hide provider-specific fields with subtle animation
   - Ensure only one set of model/temperature/max-tokens fields, always reflecting selected provider

### 5.3 Security & Validation

1. Only users with admin privileges can access and change settings
2. Validate all inputs before saving
3. Changes should take effect immediately for new AI interactions

### 5.4 Testing

- Unit tests for config update service
- Integration tests for runtime config changes
- Manual UI testing for all provider/model combinations and field visibility
