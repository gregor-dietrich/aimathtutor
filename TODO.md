# TODO - Detailed Implementation Plans

## 0. General

### Implementation Priority

Suggested order (easiest to hardest):

1. **Problem Category Selection** (Task 2 - extends generation feature)
1. **Add Rich Context to AI API Requests** (Task 6.1 - moderate complexity, improves AI responses)
1. **Multiple Problems Per Exercise** (Task 3 - moderate complexity, requires DB changes)
1. **Admin Views** (Task 4 - most complex, requires multiple new views and services)

### Testing Checklist (for each feature)

- [ ] Unit tests for service methods
- [ ] Integration tests for DB operations
- [ ] Manual UI testing in both views
- [ ] Edge cases (empty data, invalid input, etc.)
- [ ] Permission/security checks
- [ ] Performance with large datasets (admin views)

---

## 2. Problem Category Selection for Generation

**Goal:** Allow users to choose from different math problem categories instead of always generating linear equations.

**Implementation Plan:**

### 2.1 Backend Changes

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

### 2.2 Frontend Changes

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

### 3.1 Backend Changes

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

### 3.2 Frontend Changes

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

### 4.1 Backend Changes

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

### 4.2 Frontend Changes

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

## 6. AIChatPanel Experience Improvements

### 1. Add Rich Context to AI API Requests

**Goal:** Improve the relevance and personalization of AI responses by including the last 5 actions, last 5 user questions, and last 5 AI messages (feedback or answers) in every prompt sent to the AI APIs—regardless of whether the request is for tutoring feedback or a direct question.

**Implementation Plan:**

- **Backend Changes:**
  - **AITutorService:**
    - Update both `buildQuestionAnsweringPrompt()` and `buildMathTutoringPrompt()` to include:
      - The last 5 actions performed by the student
      - The last 5 user questions
      - The last 5 AI messages (feedback or answers)
    - Ensure the prompt structure is consistent and concise, and respects token limits for each provider.
  - **ChatMessageDto:**
    - Add optional `relatedAction` field to link messages to specific actions.
  - **AIInteractionEntity:**
    - Add `conversationContext` field to log the full context sent with each AI request.

- **Frontend Changes:**
  - **AIChatPanel:**
    - Maintain rolling buffers for:
      - Last 5 actions
      - Last 5 user questions
      - Last 5 AI messages
    - Pass all three buffers to the backend with each user question or action.

- **Testing:**
  - Verify that prompts include the correct context for both tutoring and questions.
  - Ensure token limits are respected for all AI providers.
  - Test with long conversations and many actions to confirm performance and accuracy.
