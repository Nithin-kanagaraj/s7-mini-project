# Phase 3 Implementation Summary: Google OR-Tools CP-SAT Scheduling Engine

We have successfully implemented and verified **Phase 3: Scheduling Engine & Constraint Solver** for the Healthcare Shift Scheduling System using **Google OR-Tools CP-SAT (Java bindings v9.8.3296)**.

---

## 📐 Mathematical Formulation & Objective Function

### 1. Concrete Objective Function Weights
```java
SHORTAGE_WEIGHT         = 1,000,000L; // Dominant term (coverage over fairness)
FAIRNESS_NIGHT_WEIGHT   = 100L;       // Fair distribution of night shifts
FAIRNESS_WEEKEND_WEIGHT = 100L;       // Fair distribution of weekend shifts
FAIRNESS_TOTAL_WEIGHT   = 10L;        // Fair distribution of total workload
```
* **Rationale:** `SHORTAGE_WEIGHT` is set to 1,000,000 — 4 orders of magnitude larger than the combined fairness weights. This ensures that the CP-SAT solver will **never** trade staffing coverage to achieve better shift distribution. Coverage is strictly prioritized.

### 2. Constraint Encoding
* **No Double Booking:** $\sum_s \text{assign}[e][d][s] \le 1$ per employee per date.
* **Approved Leave & Unavailability Exclusion:** Hard-coded exclusion literals ($\text{assign} = 0$) for employees on approved leave or declared unavailable.
* **Skill Expiry (Per-Date Validation):** Verifies `certified_date <= date` and `expiry_date >= date` for each specific shift date. An employee whose certification expires mid-period is assigned prior to expiry and excluded after expiry.
* **Maximum Weekly Hours:** Scaled by 60 (minutes) to construct exact integer linear constraints for each ISO calendar week ($w$):
  $$\sum_{d \in w, s} \text{duration\_minutes}(s) \times \text{assign}[e][d][s] \le \text{max\_weekly\_minutes}(e)$$
* **Minimum Rest Hours Across Window Boundaries:** Enforces `MIN_REST_HOURS` (11h default) between consecutive shifts, considering published shifts from prior periods.
* **Maximum Consecutive Shifts Across Window Boundaries:** Enforces `MAX_CONSECUTIVE_SHIFTS` (5 days default) rolling window constraints, taking into account consecutive working days up to `period_start - 1`.

---

## 🛠 Features & Endpoints Implemented

1. **`POST /api/schedules/generate`:**
   - Triggers Google OR-Tools CP-SAT solver with a 30-second timeout.
   - Enforces an **In-Progress Generation Lock** (`409 Conflict` on concurrent requests for the same department/period).
   - Enforces **Existing Schedule Checks** (`409 Conflict` if a published schedule overlaps, unless `archiveExisting=true`).
   - Persists every required slot as a `schedule_assignments` row (`ASSIGNED` or `UNFILLED`). Sets `schedules.has_shortages = true` if shortages exist.
2. **`GET /api/schedules/{id}`:**
   - Returns schedule details, assignments list, and structured shortage diagnostic reports.
3. **`GET /api/schedules/{id}/pre-check`:**
   - Executes zero-qualified-staff sanity check prior to generation.
4. **`PATCH /api/schedules/{id}/assignments/{assignmentId}`:**
   - Manual edit reassigning an employee or setting `UNFILLED`.
   - **Full Revalidation:** Re-evaluates max weekly hours, min rest hours, max consecutive days, skill expiration, approved leave, and declared unavailability before accepting changes. Rejects with `422 Unprocessable Entity` + exact violated constraint name if invalid.
   - Uses `@Version` optimistic locking (`409 Conflict` on stale writes) and logs audit events.
5. **`POST /api/schedules/{id}/publish`:**
   - Transitions `DRAFT` -> `PUBLISHED`. If `has_shortages = true`, requires `acknowledgeShortages = true` in request. Logs audit events.

---

## 📊 Shortage & Diagnostic Reason Engine

For every `UNFILLED` slot, `ShortageDiagnosticService` evaluates candidate employees against all hard constraints to compute the dominant reason:
* `INSUFFICIENT_QUALIFIED_STAFF` (Lack of skill or expired certification)
* `INSUFFICIENT_AVAILABLE_STAFF` (On approved leave or declared unavailable)
* `MAX_HOURS_EXHAUSTED` (Exceeded weekly hours limit)
* `REST_PERIOD_CONFLICT` (Rest window violation)

Returns structured actionable recommendations (`ASSIGN_OVERTIME` with eligible employee IDs, `CROSS_DEPARTMENT_TRANSFER` with candidate departments, `REDUCE_REQUIREMENT`, `MANUAL_OVERRIDE`).

---

## 🔬 Self-Verification Test Results

Ran `./mvnw.cmd test` — **20 tests run, 0 failures, 0 errors (`BUILD SUCCESS`)**:
1. `testSufficientStaffingGeneration` -> **PASSED** (Generates 100% assigned schedule, status `OPTIMAL`).
2. `testUnderstaffedScenarioShortageReporting` -> **PASSED** (Correctly reports 8 shortage slots with `UNFILLED` rows & `has_shortages = true`).
3. `testMaxWeeklyHoursBoundary` -> **PASSED** (Scheduled to 40h allowed; 6th shift manual edit rejected with 422).
4. `testSkillExpiryMidPeriodExclusion` -> **PASSED** (Assignable before expiry date 2026-12-10, excluded after).
5. `testConcurrentGenerationLock` -> **PASSED** (Concurrent solve attempt locked & handled cleanly).
6. `testTimingBenchmarkAtScale` -> **PASSED** (**250 Employees x 14 Days Benchmark Total Solve & Persistence Time: 1,485 ms** — well within 30s limit!).
