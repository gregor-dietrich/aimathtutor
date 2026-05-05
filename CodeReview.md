# Full Code Review & Remediation Plan

## High-Priority Issues

| #   | Issue                                                                                | Location                     | Action                                                                      | Status                                                                                                                                      |
| --- | ------------------------------------------------------------------------------------ | ---------------------------- | --------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| H1  | **N+1 query in `LessonService.isDescendantOf`** — lazy-loads parent chain one-by-one | `LessonService.java:203-211` | Write recursive CTE query or use bounded eager fetch                        | **DEFERRED** — CTE is a major refactor; tree depth is bounded in practice                                                                   |
| H3  | **No declarative auth annotations** (`@RolesAllowed`, `@Authenticated`) on any route | All 16 route views           | Add `@Authenticated` to user views, `@RolesAllowed("admin")` to admin views | **DEFERRED** — Vaadin-Quarkus security annotation integration unclear; current centralized layout-based enforcement is functionally correct |

## Medium-Priority Issues

| #   | Issue                                                                                                                               | Location                             | Status                                                                                                                                                                          |
| --- | ----------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| M9  | **Sync service calls in `ExerciseWorkspaceView.beforeEnter`/`initializeView`** — exercise lookup + session creation block UI thread | `ExerciseWorkspaceView.java:118,147` | **DEFERRED** — exercise lookup is required for routing decisions before UI render; session creation is intrinsic to workspace setup. Refactor would require deferred state init |
| M17 | **Inconsistent `initialized` flag pattern** across views                                                                            | Multiple                             | **DEFERRED** — content views deliberately rebuild on navigation; standardizing would break refresh-on-nav semantics                                                             |

## CI

| #   | Issue                                                                       | Action                                                  |
| --- | --------------------------------------------------------------------------- | ------------------------------------------------------- |
| T6  | **CI pipeline missing OWASP dep-check and secret scanning** (commented out) | Uncomment and configure `NVD_API_KEY` repository secret |
| T7  | **CI security job lacks Maven cache**                                       | Add `actions/cache@v4` to security job                  |

## Low-Priority Issues

| #   | Issue                         | Action                                                                                                                                                                              |
| --- | ----------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| L4  | No session timeout configured | ~~Add `quarkus.http.session-timeout=30M`~~ — **NOTE:** suggested config property does not exist in Quarkus 3.33, need to find another way to handle this, or can we leave it as is? |

---
