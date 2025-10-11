# Graspable Math Views - Architecture Overview

**Date:** October 11, 2025

## Current State: Two Views with Different Purposes

### 1. GraspableMathView (Standalone Workspace) ✅
**Route:** `/graspable-math`  
**Purpose:** Free-form math practice without exercises  
**Status:** ✅ UPDATED & WORKING

#### Architecture
- ✅ Uses external JavaScript file (`graspable-math-init.js`)
- ✅ Loads `gmath-3.5.13.min.js` directly
- ✅ Uses `canvas.model.createElement()` API
- ✅ No session/exercise dependency
- ✅ Generates random problems with AI
- ✅ Standalone practice tool

#### Dependencies
- `AuthService` - User identification
- `AITutorService` - Problem generation & feedback
- ❌ No `GraspableMathService` (removed)
- ❌ No `ExerciseService` (not needed)

---

### 2. ExerciseWorkspaceView (Exercise-Specific) ⚠️
**Route:** `/exercise/:exerciseId`  
**Purpose:** Work on specific assigned exercises with tracking  
**Status:** ⚠️ NEEDS UPDATE

#### Current Issues
1. ❌ Uses old `gm-inject.js` library (deprecated approach)
2. ❌ Uses `GraspableMath.Canvas` API (wrong namespace)
3. ❌ Inline JavaScript (hard to maintain)
4. ❌ Different initialization than GraspableMathView
5. ⚠️ May have same blank canvas issues we just fixed

#### Architecture (Current)
```java
@JavaScript("https://graspablemath.com/shared/libs/gmath/gm-inject.js")

// Inline JavaScript in Java:
UI.getCurrent().getPage().executeJs(
    "const canvas = new GraspableMath.Canvas(...)"  // Wrong API!
);
```

#### Dependencies
- `AuthService` - User identification
- `AITutorService` - AI feedback
- `ExerciseService` - Load exercise data
- ✅ `GraspableMathService` - Session tracking (CORRECT - this view needs it!)

---

## Comparison Table

| Feature | GraspableMathView | ExerciseWorkspaceView |
|---------|-------------------|----------------------|
| **Purpose** | Free practice | Assigned exercises |
| **Library** | ✅ gmath-3.5.13.min.js | ❌ gm-inject.js (old) |
| **API** | ✅ `canvas.model.createElement()` | ❌ `GraspableMath.Canvas` |
| **JavaScript** | ✅ External file | ❌ Inline code |
| **Session Tracking** | ❌ None (not needed) | ✅ Yes (needed!) |
| **Exercise Link** | ❌ None | ✅ Yes |
| **Problem Source** | AI-generated random | Pre-configured exercise |
| **Status** | ✅ Working | ⚠️ Needs update |

---

## Recommended Actions

### Option 1: Update ExerciseWorkspaceView to Use Same Library ✅ RECOMMENDED

**Advantages:**
- Consistent codebase
- Reuse working JavaScript file
- Same API across both views
- Easier maintenance

**Changes Needed:**
1. Remove `@JavaScript("gm-inject.js")` annotation
2. Use `graspable-math-init.js` (or create `exercise-graspable-math-init.js`)
3. Update to use `canvas.model.createElement()` API
4. Keep `GraspableMathService` for session tracking
5. Load exercise-specific problems instead of random

**Code Structure:**
```java
// Remove old annotation
// @JavaScript("https://graspablemath.com/shared/libs/gmath/gm-inject.js")

private void initializeGraspableMath() {
    // Load the same external JS file
    UI.getCurrent().getPage().addJavaScript("/js/graspable-math-init.js");
    
    // Call initialization with exercise data
    UI.getCurrent().getPage().executeJs("""
        setTimeout(function() {
            if (window.graspableMathUtils) {
                window.graspableMathUtils.loadProblem('%s', 100, 50);
            }
        }, 500);
        """, exercise.graspableInitialExpression);
}
```

---

### Option 2: Keep Separate (Not Recommended)

**Disadvantages:**
- Duplicate Graspable Math integration code
- Two different APIs to maintain
- ExerciseWorkspaceView may have same bugs we just fixed
- Inconsistent user experience

---

## Proposed Unified Architecture

### Shared JavaScript Module
Create a **shared Graspable Math module** that both views can use:

```
/src/main/resources/META-INF/resources/js/
├── graspable-math-core.js       # Core initialization & utilities
├── graspable-math-standalone.js # Extensions for standalone mode
└── graspable-math-exercise.js   # Extensions for exercise mode
```

### View-Specific Responsibilities

**GraspableMathView (Standalone):**
- No session tracking
- AI-generated random problems
- Practice mode
- No progress tracking

**ExerciseWorkspaceView (Exercise-based):**
- ✅ Session tracking via `GraspableMathService`
- ✅ Exercise-specific problems
- ✅ Progress tracking
- ✅ Completion detection
- ✅ Score calculation

---

## Action Plan

### Phase 1: Update ExerciseWorkspaceView ⚠️ HIGH PRIORITY
1. [ ] Replace `gm-inject.js` with `gmath-3.5.13.min.js`
2. [ ] Update JavaScript to use external file
3. [ ] Fix API calls (`GraspableMath` → `gmath`)
4. [ ] Test with actual exercises
5. [ ] Verify session tracking still works

### Phase 2: Refactor for Code Reuse
1. [ ] Extract common Graspable Math code to shared module
2. [ ] Create view-specific extensions
3. [ ] Update both views to use shared code
4. [ ] Document the architecture

### Phase 3: Testing
1. [ ] Test GraspableMathView (standalone practice)
2. [ ] Test ExerciseWorkspaceView (assigned exercises)
3. [ ] Verify session tracking in exercise mode
4. [ ] Verify AI feedback in both modes
5. [ ] Test exercise completion detection

---

## Key Differences to Preserve

### GraspableMathView
```java
// NO session tracking
event.studentId = authService.getUserId();
// No exerciseId, no sessionId

// AI generates problems
aiTutorService.generateProblem("intermediate", "algebra");
```

### ExerciseWorkspaceView
```java
// WITH session tracking
event.studentId = authService.getUserId();
event.exerciseId = exerciseId;
event.sessionId = currentSessionId;

// Exercise provides problem
graspableMathService.processEvent(event);
```

---

## Questions to Answer

1. **Does ExerciseWorkspaceView currently work?**
   - Need to test with actual exercises
   - May have same blank canvas issues

2. **Are exercises being used in production?**
   - If yes, update is CRITICAL
   - If no, can refactor at leisure

3. **Should we merge the views?**
   - Could have one view with optional exerciseId
   - Cleaner architecture
   - But mixing concerns might be confusing

4. **Session tracking strategy?**
   - GraspableMathView: No tracking (practice only)
   - ExerciseWorkspaceView: Full tracking (graded work)

---

## Recommendation Summary

### Immediate Action Required ⚠️
**Update ExerciseWorkspaceView** to use the same Graspable Math integration we just fixed in GraspableMathView:

1. Same library (gmath-3.5.13.min.js)
2. Same API (canvas.model.createElement)
3. Same external JavaScript approach
4. Keep exercise/session tracking functionality

### Benefits
- ✅ Consistent codebase
- ✅ Reuse working solution
- ✅ Easier maintenance
- ✅ Same bug fixes apply to both
- ✅ Better user experience

### Timeline
- **High Priority** if exercises are in use
- **Medium Priority** if exercises are not yet deployed
- **Estimated Time:** 2-3 hours to update and test

---

## Next Steps

1. Determine if ExerciseWorkspaceView is actively used
2. Test current ExerciseWorkspaceView functionality
3. Apply same fixes as GraspableMathView
4. Verify session tracking still works
5. Document the unified architecture
