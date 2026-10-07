package com.hospital.scheduling.schedule.engine;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import com.hospital.scheduling.employee.Employee;
import com.hospital.scheduling.employee.EmployeeType;
import com.hospital.scheduling.leave.LeaveRequest;
import com.hospital.scheduling.availability.Availability;
import com.hospital.scheduling.shift.ShiftTemplate;
import com.hospital.scheduling.shift.StaffingRequirement;
import com.hospital.scheduling.skill.EmployeeSkill;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Component
public class OrToolsScheduleEngine {

    static {
        Loader.loadNativeLibraries();
    }

    private static final long SHORTAGE_WEIGHT = 1_000_000L;
    private static final long FAIRNESS_NIGHT_WEIGHT = 100L;
    private static final long FAIRNESS_WEEKEND_WEIGHT = 100L;
    private static final long FAIRNESS_TOTAL_WEIGHT = 10L;

    @Getter
    @Builder
    public static class SolveInput {
        private String departmentId;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private double maxWeeklyHoursDefault;
        private double minRestHoursDefault;
        private int maxConsecutiveShiftsDefault;
        private List<Employee> candidateEmployees;
        private List<ShiftTemplate> shiftTemplates;
        private List<StaffingRequirement> staffingRequirements;
        private List<LeaveRequest> approvedLeaves;
        private List<Availability> unavailabilities;
        private Map<String, List<EmployeeSkill>> employeeSkillsMap;
        private Map<String, List<PastAssignment>> pastAssignmentsMap;
    }

    @Getter
    @Builder
    public static class PastAssignment {
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private boolean isNight;
        private boolean isWeekend;
    }

    @Getter
    @Builder
    public static class ShiftAssignmentResult {
        private String requirementId;
        private String employeeId;
        private String shiftTemplateId;
        private LocalDate date;
        private boolean isShortage;
    }

    @Getter
    @Builder
    public static class SolveOutput {
        private String solveStatus; // OPTIMAL, FEASIBLE, TIMEOUT_PARTIAL, INFEASIBLE
        private long solveTimeMs;
        private List<ShiftAssignmentResult> assignments;
        private Map<StaffingRequirement, Integer> shortagesMap;
    }

    public SolveOutput solve(SolveInput input) {
        long startTimeMs = System.currentTimeMillis();

        CpModel model = new CpModel();
        List<LocalDate> dates = getDatesBetween(input.getPeriodStart(), input.getPeriodEnd());

        List<Employee> employees = input.getCandidateEmployees();
        List<ShiftTemplate> shiftTemplates = input.getShiftTemplates();
        List<StaffingRequirement> requirements = input.getStaffingRequirements();

        // 1. Map of assign[e][d][s] variables
        Map<String, BoolVar> assignVars = new HashMap<>();
        Map<String, Boolean> isExcludedMap = new HashMap<>();

        for (Employee e : employees) {
            for (LocalDate d : dates) {
                for (ShiftTemplate s : shiftTemplates) {
                    String key = makeKey(e.getId(), d, s.getId());

                    boolean excluded = checkExclusions(e, d, s, input);
                    isExcludedMap.put(key, excluded);

                    BoolVar var = model.newBoolVar(key);
                    if (excluded) {
                        model.addEquality(var, 0);
                    }
                    assignVars.put(key, var);
                }
            }
        }

        // 2. Constraint: No double-booking per employee per date
        for (Employee e : employees) {
            for (LocalDate d : dates) {
                LinearExprBuilder daySum = LinearExpr.newBuilder();
                for (ShiftTemplate s : shiftTemplates) {
                    daySum.add(assignVars.get(makeKey(e.getId(), d, s.getId())));
                }
                model.addLessOrEqual(daySum, 1);
            }
        }

        // 3. Staffing Requirements Demand Constraints with Shortage Slack Variables
        Map<StaffingRequirement, IntVar> shortageVarsMap = new HashMap<>();

        for (StaffingRequirement req : requirements) {
            int requiredCount = req.getRequiredCount();
            LocalDate reqDate = req.getShiftDate();
            ShiftTemplate reqShift = req.getShiftTemplate();
            EmployeeType reqType = req.getEmployeeType();
            String reqSkillId = req.getRequiredSkill() != null ? req.getRequiredSkill().getId() : null;

            LinearExprBuilder demandBuilder = LinearExpr.newBuilder();

            for (Employee e : employees) {
                if (e.getEmployeeType() != reqType) continue;
                if (reqSkillId != null && !isSkillValidOnDate(input.getEmployeeSkillsMap(), e.getId(), reqSkillId, reqDate)) continue;

                String key = makeKey(e.getId(), reqDate, reqShift.getId());
                demandBuilder.add(assignVars.get(key));
            }

            IntVar shortageVar = model.newIntVar(0, Math.max(0, requiredCount), "shortage_" + req.getId());
            shortageVarsMap.put(req, shortageVar);

            demandBuilder.add(shortageVar);
            model.addGreaterOrEqual(demandBuilder, requiredCount);
        }

        // 4. Maximum Weekly Hours Constraint per Employee per ISO Calendar Week
        Map<Integer, List<LocalDate>> isoWeeks = groupDatesByIsoWeek(dates);
        for (Employee e : employees) {
            double maxWeeklyHours = e.getMaxWeeklyHoursOverride() != null
                    ? e.getMaxWeeklyHoursOverride().doubleValue()
                    : input.getMaxWeeklyHoursDefault();
            long maxWeeklyMinutes = (long) (maxWeeklyHours * 60.0);

            for (Map.Entry<Integer, List<LocalDate>> entry : isoWeeks.entrySet()) {
                List<LocalDate> weekDates = entry.getValue();
                LinearExprBuilder weekBuilder = LinearExpr.newBuilder();

                for (LocalDate d : weekDates) {
                    for (ShiftTemplate s : shiftTemplates) {
                        String key = makeKey(e.getId(), d, s.getId());
                        long shiftMinutes = Math.round(s.getDurationHours().doubleValue() * 60.0);
                        weekBuilder.addTerm(assignVars.get(key), shiftMinutes);
                    }
                }

                model.addLessOrEqual(weekBuilder, maxWeeklyMinutes);
            }
        }

        // 5. Minimum Rest Hours Constraint (Including Boundary Checks)
        double minRestHours = input.getMinRestHoursDefault();
        for (Employee e : employees) {
            List<PastAssignment> pasts = input.getPastAssignmentsMap().getOrDefault(e.getId(), Collections.emptyList());

            for (LocalDate d : dates) {
                for (ShiftTemplate s : shiftTemplates) {
                    String key = makeKey(e.getId(), d, s.getId());
                    if (Boolean.TRUE.equals(isExcludedMap.get(key))) continue;

                    LocalDateTime currentStart = d.atTime(s.getStartTime());

                    for (PastAssignment past : pasts) {
                        LocalDateTime pastEnd = past.getDate().atTime(past.getEndTime());
                        if (past.getEndTime().isBefore(past.getStartTime())) {
                            pastEnd = past.getDate().plusDays(1).atTime(past.getEndTime());
                        }

                        if (!currentStart.isBefore(pastEnd)) {
                            double hoursBetween = ChronoUnit.MINUTES.between(pastEnd, currentStart) / 60.0;
                            if (hoursBetween < minRestHours) {
                                model.addEquality(assignVars.get(key), 0);
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < dates.size(); i++) {
                LocalDate d1 = dates.get(i);
                for (ShiftTemplate s1 : shiftTemplates) {
                    String key1 = makeKey(e.getId(), d1, s1.getId());
                    LocalDateTime end1 = d1.atTime(s1.getEndTime());
                    if (s1.getEndTime().isBefore(s1.getStartTime())) {
                        end1 = d1.plusDays(1).atTime(s1.getEndTime());
                    }

                    for (int j = i; j < Math.min(dates.size(), i + 2); j++) {
                        LocalDate d2 = dates.get(j);
                        for (ShiftTemplate s2 : shiftTemplates) {
                            if (d1.equals(d2) && s1.getId().equals(s2.getId())) continue;

                            LocalDateTime start2 = d2.atTime(s2.getStartTime());
                            if (!start2.isBefore(end1)) {
                                double rest = ChronoUnit.MINUTES.between(end1, start2) / 60.0;
                                if (rest < minRestHours) {
                                    LinearExprBuilder restPair = LinearExpr.newBuilder();
                                    restPair.add(assignVars.get(key1));
                                    restPair.add(assignVars.get(makeKey(e.getId(), d2, s2.getId())));
                                    model.addLessOrEqual(restPair, 1);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Maximum Consecutive Working Days Constraint (Including Boundary Checks)
        int maxConsecutive = input.getMaxConsecutiveShiftsDefault();
        for (Employee e : employees) {
            List<PastAssignment> pasts = input.getPastAssignmentsMap().getOrDefault(e.getId(), Collections.emptyList());
            int pastConsecutive = calculatePastConsecutiveDays(pasts, input.getPeriodStart());

            Map<LocalDate, LinearExprBuilder> workedMap = new HashMap<>();
            for (LocalDate d : dates) {
                LinearExprBuilder dayWorked = LinearExpr.newBuilder();
                for (ShiftTemplate s : shiftTemplates) {
                    dayWorked.add(assignVars.get(makeKey(e.getId(), d, s.getId())));
                }
                workedMap.put(d, dayWorked);
            }

            if (pastConsecutive > 0) {
                int maxAllowedAtStart = Math.max(0, maxConsecutive - pastConsecutive);
                LinearExprBuilder startWorked = LinearExpr.newBuilder();
                for (int k = 0; k <= maxAllowedAtStart && k < dates.size(); k++) {
                    startWorked.add(workedMap.get(dates.get(k)));
                }
                model.addLessOrEqual(startWorked, maxAllowedAtStart);
            }

            int windowSize = maxConsecutive + 1;
            for (int i = 0; i <= dates.size() - windowSize; i++) {
                LinearExprBuilder windowWorked = LinearExpr.newBuilder();
                for (int k = 0; k < windowSize; k++) {
                    windowWorked.add(workedMap.get(dates.get(i + k)));
                }
                model.addLessOrEqual(windowWorked, maxConsecutive);
            }
        }

        // 7. Objective Function Formulation
        LinearExprBuilder objBuilder = LinearExpr.newBuilder();

        // Shortage term
        for (IntVar shortageVar : shortageVarsMap.values()) {
            objBuilder.addTerm(shortageVar, SHORTAGE_WEIGHT);
        }

        // Fairness terms
        for (Employee e : employees) {
            for (LocalDate d : dates) {
                boolean isWeekend = ShiftClassification.isWeekend(d);
                for (ShiftTemplate s : shiftTemplates) {
                    String key = makeKey(e.getId(), d, s.getId());
                    BoolVar var = assignVars.get(key);

                    objBuilder.addTerm(var, FAIRNESS_TOTAL_WEIGHT);
                    if (isWeekend) objBuilder.addTerm(var, FAIRNESS_WEEKEND_WEIGHT);
                    if (ShiftClassification.isNight(s)) {
                        objBuilder.addTerm(var, FAIRNESS_NIGHT_WEIGHT);
                    }
                }
            }
        }

        model.minimize(objBuilder);

        // 8. Solve with 30-Second Timeout
        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(30.0);

        CpSolverStatus status = solver.solve(model);
        long solveTimeMs = System.currentTimeMillis() - startTimeMs;

        String solveStatus;
        if (status == CpSolverStatus.OPTIMAL) {
            solveStatus = "OPTIMAL";
        } else if (status == CpSolverStatus.FEASIBLE) {
            solveStatus = (solveTimeMs >= 30000) ? "TIMEOUT_PARTIAL" : "FEASIBLE";
        } else {
            solveStatus = "INFEASIBLE";
        }

        log.info("CP-SAT Solve finished in {} ms with status: {}", solveTimeMs, solveStatus);

        // 9. Extract Assignment Results & Shortages Map
        List<ShiftAssignmentResult> results = new ArrayList<>();
        Map<StaffingRequirement, Integer> shortagesMap = new HashMap<>();

        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            for (StaffingRequirement req : requirements) {
                int requiredCount = req.getRequiredCount();
                LocalDate reqDate = req.getShiftDate();
                ShiftTemplate reqShift = req.getShiftTemplate();
                EmployeeType reqType = req.getEmployeeType();
                String reqSkillId = req.getRequiredSkill() != null ? req.getRequiredSkill().getId() : null;

                int assignedCount = 0;
                for (Employee e : employees) {
                    if (e.getEmployeeType() != reqType) continue;
                    if (reqSkillId != null && !isSkillValidOnDate(input.getEmployeeSkillsMap(), e.getId(), reqSkillId, reqDate)) continue;

                    String key = makeKey(e.getId(), reqDate, reqShift.getId());
                    BoolVar var = assignVars.get(key);
                    if (solver.booleanValue(var)) {
                        results.add(ShiftAssignmentResult.builder()
                                .requirementId(req.getId())
                                .employeeId(e.getId())
                                .shiftTemplateId(reqShift.getId())
                                .date(reqDate)
                                .isShortage(false)
                                .build());
                        assignedCount++;
                    }
                }

                int shortage = Math.max(0, requiredCount - assignedCount);
                shortagesMap.put(req, shortage);

                for (int i = 0; i < shortage; i++) {
                    results.add(ShiftAssignmentResult.builder()
                            .requirementId(req.getId())
                            .employeeId(null)
                            .shiftTemplateId(reqShift.getId())
                            .date(reqDate)
                            .isShortage(true)
                            .build());
                }
            }
        }

        return SolveOutput.builder()
                .solveStatus(solveStatus)
                .solveTimeMs(solveTimeMs)
                .assignments(results)
                .shortagesMap(shortagesMap)
                .build();
    }

    private boolean checkExclusions(Employee e, LocalDate d, ShiftTemplate s, SolveInput input) {
        for (LeaveRequest leave : input.getApprovedLeaves()) {
            if (leave.getEmployee().getId().equals(e.getId())) {
                if (!d.isBefore(leave.getStartDate()) && !d.isAfter(leave.getEndDate())) {
                    return true;
                }
            }
        }

        for (Availability avail : input.getUnavailabilities()) {
            if (avail.getEmployee().getId().equals(e.getId()) && avail.getUnavailableDate().equals(d)) {
                if (avail.getShiftTemplate() == null || avail.getShiftTemplate().getId().equals(s.getId())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isSkillValidOnDate(Map<String, List<EmployeeSkill>> skillsMap, String employeeId, String requiredSkillId, LocalDate date) {
        List<EmployeeSkill> list = skillsMap.getOrDefault(employeeId, Collections.emptyList());
        for (EmployeeSkill es : list) {
            if (es.getSkill().getId().equals(requiredSkillId)) {
                boolean certified = es.getCertifiedDate() == null || !es.getCertifiedDate().isAfter(date);
                boolean notExpired = es.getExpiryDate() == null || !es.getExpiryDate().isBefore(date);
                if (certified && notExpired) {
                    return true;
                }
            }
        }
        return false;
    }

    private int calculatePastConsecutiveDays(List<PastAssignment> pasts, LocalDate periodStart) {
        if (pasts == null || pasts.isEmpty()) return 0;
        int count = 0;
        LocalDate checkDate = periodStart.minusDays(1);
        Set<LocalDate> pastDates = new HashSet<>();
        for (PastAssignment p : pasts) {
            pastDates.add(p.getDate());
        }

        while (pastDates.contains(checkDate)) {
            count++;
            checkDate = checkDate.minusDays(1);
        }
        return count;
    }

    private Map<Integer, List<LocalDate>> groupDatesByIsoWeek(List<LocalDate> dates) {
        Map<Integer, List<LocalDate>> weeks = new HashMap<>();
        for (LocalDate d : dates) {
            int weekNum = d.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            int year = d.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR);
            int key = year * 100 + weekNum;
            weeks.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
        }
        return weeks;
    }

    private List<LocalDate> getDatesBetween(LocalDate start, LocalDate end) {
        List<LocalDate> list = new ArrayList<>();
        LocalDate curr = start;
        while (!curr.isAfter(end)) {
            list.add(curr);
            curr = curr.plusDays(1);
        }
        return list;
    }

    private String makeKey(String empId, LocalDate date, String shiftId) {
        return empId + ":" + date + ":" + shiftId;
    }
}
