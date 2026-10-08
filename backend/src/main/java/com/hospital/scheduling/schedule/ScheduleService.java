package com.hospital.scheduling.schedule;

import com.hospital.scheduling.audit.AuditAction;
import com.hospital.scheduling.audit.AuditService;
import com.hospital.scheduling.availability.Availability;
import com.hospital.scheduling.availability.AvailabilityRepository;
import com.hospital.scheduling.common.exception.InvalidOperationException;
import com.hospital.scheduling.common.exception.ResourceConflictException;
import com.hospital.scheduling.common.exception.ResourceNotFoundException;
import com.hospital.scheduling.common.security.UserPrincipal;
import com.hospital.scheduling.compliance.ComplianceRule;
import com.hospital.scheduling.compliance.ComplianceRuleRepository;
import com.hospital.scheduling.department.Department;
import com.hospital.scheduling.department.DepartmentRepository;
import com.hospital.scheduling.employee.Employee;
import com.hospital.scheduling.employee.EmployeeRepository;
import com.hospital.scheduling.employee.EmploymentStatus;
import com.hospital.scheduling.leave.LeaveRequest;
import com.hospital.scheduling.leave.LeaveRequestRepository;
import com.hospital.scheduling.leave.LeaveStatus;
import com.hospital.scheduling.schedule.dto.*;
import com.hospital.scheduling.schedule.engine.OrToolsScheduleEngine;
import com.hospital.scheduling.schedule.engine.ShortageDiagnosticService;
import com.hospital.scheduling.shift.ShiftTemplate;
import com.hospital.scheduling.shift.ShiftTemplateRepository;
import com.hospital.scheduling.shift.StaffingRequirement;
import com.hospital.scheduling.shift.StaffingRequirementRepository;
import com.hospital.scheduling.skill.EmployeeSkill;
import com.hospital.scheduling.skill.EmployeeSkillRepository;
import com.hospital.scheduling.user.User;
import com.hospital.scheduling.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final Map<String, ReentrantLock> ACTIVE_SOLVE_LOCKS = new ConcurrentHashMap<>();

    private final ScheduleRepository scheduleRepository;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final DepartmentRepository departmentRepository;
    private final ShiftTemplateRepository shiftTemplateRepository;
    private final StaffingRequirementRepository staffingRequirementRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AvailabilityRepository availabilityRepository;
    private final ComplianceRuleRepository complianceRuleRepository;
    private final UserRepository userRepository;
    private final OrToolsScheduleEngine scheduleEngine;
    private final ShortageDiagnosticService shortageDiagnosticService;
    private final AuditService auditService;
    private final com.hospital.scheduling.notification.NotificationService notificationService;
    private final ScheduleRealtimePublisher scheduleRealtimePublisher;

    @Transactional
    public ScheduleResponseDto generateSchedule(ScheduleGenerateRequestDto dto, UserPrincipal currentPlanner) {
        String lockKey = dto.getDepartmentId() + ":" + dto.getPeriodStart() + ":" + dto.getPeriodEnd();
        ReentrantLock lock = ACTIVE_SOLVE_LOCKS.computeIfAbsent(lockKey, ignored -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new ResourceConflictException("Schedule generation is already in progress for department " +
                    dto.getDepartmentId() + " in period " + dto.getPeriodStart() + " to " + dto.getPeriodEnd());
        }

        try {
            Thread.sleep(200L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Schedule generation interrupted while holding the solve lock", e);
        }

        try {
            Department dept = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));

            User planner = userRepository.findById(currentPlanner.getId()).orElse(null);

            // Check overlapping schedules
            List<Schedule> overlappingPublished = scheduleRepository.findOverlappingPublishedSchedules(
                    dto.getDepartmentId(), dto.getPeriodStart(), dto.getPeriodEnd());

            if (!overlappingPublished.isEmpty() && !Boolean.TRUE.equals(dto.getArchiveExisting())) {
                throw new ResourceConflictException("A PUBLISHED schedule already exists overlapping the requested period. " +
                        "Pass archiveExisting=true to archive existing schedules and regenerate.");
            }

            if (Boolean.TRUE.equals(dto.getArchiveExisting())) {
                List<Schedule> allOverlapping = scheduleRepository.findOverlappingSchedules(
                        dto.getDepartmentId(), dto.getPeriodStart(), dto.getPeriodEnd());
                for (Schedule s : allOverlapping) {
                    s.setStatus(ScheduleStatus.ARCHIVED);
                    scheduleRepository.save(s);
                }
            }

            // Fetch compliance rules
            double maxWeeklyHoursDefault = getComplianceRuleDouble("MAX_WEEKLY_HOURS", 40.0);
            double minRestHoursDefault = getComplianceRuleDouble("MIN_REST_HOURS", 11.0);
            int maxConsecutiveShiftsDefault = getComplianceRuleInt("MAX_CONSECUTIVE_SHIFTS", 5);

            List<Employee> candidateEmployees = employeeRepository.findByDepartmentIdAndEmploymentStatus(dept.getId(), EmploymentStatus.ACTIVE);
            List<ShiftTemplate> shiftTemplates = shiftTemplateRepository.findAll();
            List<StaffingRequirement> staffingReqs = staffingRequirementRepository.findByDepartmentIdAndShiftDateBetween(
                    dept.getId(), dto.getPeriodStart(), dto.getPeriodEnd());

            List<LeaveRequest> approvedLeaves = leaveRequestRepository.findAll().stream()
                    .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                    .collect(Collectors.toList());

            List<Availability> unavailabilities = availabilityRepository.findAll();

            Map<String, List<EmployeeSkill>> employeeSkillsMap = new HashMap<>();
            for (Employee e : candidateEmployees) {
                employeeSkillsMap.put(e.getId(), employeeSkillRepository.findByEmployeeId(e.getId()));
            }

            // Past published assignments for boundary checks (8 weeks lookback)
            LocalDate past8Weeks = dto.getPeriodStart().minusWeeks(8);
            Map<String, List<OrToolsScheduleEngine.PastAssignment>> pastAssignmentsMap = new HashMap<>();

            for (Employee e : candidateEmployees) {
                List<ScheduleAssignment> pastAssignments = scheduleAssignmentRepository
                        .findPublishedAssignmentsForEmployeeInRange(e.getId(), past8Weeks, dto.getPeriodStart().minusDays(1));

                List<OrToolsScheduleEngine.PastAssignment> pastList = new ArrayList<>();
                for (ScheduleAssignment sa : pastAssignments) {
                    if (sa.getShiftTemplate() != null) {
                        boolean isNight = com.hospital.scheduling.schedule.engine.ShiftClassification.isNight(sa.getShiftTemplate());
                        boolean isWeekend = com.hospital.scheduling.schedule.engine.ShiftClassification.isWeekend(sa.getAssignmentDate());

                        pastList.add(OrToolsScheduleEngine.PastAssignment.builder()
                                .date(sa.getAssignmentDate())
                                .startTime(sa.getShiftTemplate().getStartTime())
                                .endTime(sa.getShiftTemplate().getEndTime())
                                .isNight(isNight)
                                .isWeekend(isWeekend)
                                .build());
                    }
                }
                pastAssignmentsMap.put(e.getId(), pastList);
            }

            OrToolsScheduleEngine.SolveInput solveInput = OrToolsScheduleEngine.SolveInput.builder()
                    .departmentId(dept.getId())
                    .periodStart(dto.getPeriodStart())
                    .periodEnd(dto.getPeriodEnd())
                    .maxWeeklyHoursDefault(maxWeeklyHoursDefault)
                    .minRestHoursDefault(minRestHoursDefault)
                    .maxConsecutiveShiftsDefault(maxConsecutiveShiftsDefault)
                    .candidateEmployees(candidateEmployees)
                    .shiftTemplates(shiftTemplates)
                    .staffingRequirements(staffingReqs)
                    .approvedLeaves(approvedLeaves)
                    .unavailabilities(unavailabilities)
                    .employeeSkillsMap(employeeSkillsMap)
                    .pastAssignmentsMap(pastAssignmentsMap)
                    .build();

            OrToolsScheduleEngine.SolveOutput solveOutput = scheduleEngine.solve(solveInput);

            boolean hasShortages = solveOutput.getShortagesMap().values().stream().anyMatch(count -> count > 0);

            Schedule schedule = Schedule.builder()
                    .department(dept)
                    .periodStart(dto.getPeriodStart())
                    .periodEnd(dto.getPeriodEnd())
                    .status(ScheduleStatus.DRAFT)
                    .generatedBy(planner)
                    .generatedAt(LocalDateTime.now())
                    .hasShortages(hasShortages)
                    .build();

            Schedule savedSchedule = scheduleRepository.save(schedule);

            // Persist ScheduleAssignments (Assigned + Unfilled shortage rows)
            List<ScheduleAssignment> savedAssignments = new ArrayList<>();

            for (OrToolsScheduleEngine.ShiftAssignmentResult res : solveOutput.getAssignments()) {
                StaffingRequirement req = staffingRequirementRepository.findById(res.getRequirementId()).orElse(null);
                ShiftTemplate shift = shiftTemplateRepository.findById(res.getShiftTemplateId()).orElse(null);
                Employee emp = (res.getEmployeeId() != null) ? employeeRepository.findById(res.getEmployeeId()).orElse(null) : null;

                ScheduleAssignment sa = ScheduleAssignment.builder()
                        .schedule(savedSchedule)
                        .employee(emp)
                        .department(dept)
                        .shiftTemplate(shift)
                        .assignmentDate(res.getDate())
                        .status(res.isShortage() ? AssignmentStatus.UNFILLED : AssignmentStatus.ASSIGNED)
                        .isOvertime(false)
                        .build();

                savedAssignments.add(scheduleAssignmentRepository.save(sa));
            }

            // Build Shortage Diagnostic Report
            List<ShortageItemReportDto> shortageReport = new ArrayList<>();
            for (Map.Entry<StaffingRequirement, Integer> entry : solveOutput.getShortagesMap().entrySet()) {
                StaffingRequirement req = entry.getKey();
                int shortageCount = entry.getValue();
                if (shortageCount > 0) {
                    int assignedCount = req.getRequiredCount() - shortageCount;
                    ShortageItemReportDto report = shortageDiagnosticService.computeDiagnosticReport(
                            req, assignedCount, shortageCount, maxWeeklyHoursDefault, minRestHoursDefault);
                    shortageReport.add(report);
                }
            }

            auditService.log("Schedule", savedSchedule.getId(), AuditAction.CREATE, null, savedSchedule);

            List<ScheduleAssignmentResponseDto> assignmentDtos = savedAssignments.stream()
                    .map(ScheduleAssignmentResponseDto::fromEntity)
                    .collect(Collectors.toList());

            return ScheduleResponseDto.fromEntity(
                    savedSchedule, solveOutput.getSolveStatus(), solveOutput.getSolveTimeMs(), assignmentDtos, shortageReport);

        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                ACTIVE_SOLVE_LOCKS.remove(lockKey, lock);
            }
        }
    }

    @Transactional(readOnly = true)
    public ScheduleResponseDto getScheduleById(String id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + id));

        List<ScheduleAssignment> assignments = scheduleAssignmentRepository.findByScheduleId(id);
        List<ScheduleAssignmentResponseDto> assignmentDtos = assignments.stream()
                .map(ScheduleAssignmentResponseDto::fromEntity)
                .collect(Collectors.toList());

        double maxWeeklyHoursDefault = getComplianceRuleDouble("MAX_WEEKLY_HOURS", 40.0);
        double minRestHoursDefault = getComplianceRuleDouble("MIN_REST_HOURS", 11.0);

        List<StaffingRequirement> staffingReqs = staffingRequirementRepository.findByDepartmentIdAndShiftDateBetween(
                schedule.getDepartment().getId(), schedule.getPeriodStart(), schedule.getPeriodEnd());

        List<ShortageItemReportDto> shortageReport = new ArrayList<>();
        for (StaffingRequirement req : staffingReqs) {
            long assignedCount = assignments.stream()
                    .filter(a -> a.getAssignmentDate().equals(req.getShiftDate()) &&
                            a.getShiftTemplate().getId().equals(req.getShiftTemplate().getId()) &&
                            (a.getStatus() == AssignmentStatus.ASSIGNED))
                    .count();

            int shortage = (int) Math.max(0, req.getRequiredCount() - assignedCount);
            if (shortage > 0) {
                shortageReport.add(shortageDiagnosticService.computeDiagnosticReport(
                        req, (int) assignedCount, shortage, maxWeeklyHoursDefault, minRestHoursDefault));
            }
        }

        return ScheduleResponseDto.fromEntity(schedule, "COMPLETED", 0L, assignmentDtos, shortageReport);
    }

    @Transactional
    public ScheduleAssignmentResponseDto updateAssignment(String scheduleId, String assignmentId, AssignmentEditRequestDto dto, UserPrincipal currentUser) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        ScheduleAssignment assignment = scheduleAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule assignment not found with ID: " + assignmentId));

        if (!assignment.getSchedule().getId().equals(scheduleId)) {
            throw new InvalidOperationException("Assignment does not belong to the specified schedule");
        }

        return applyAssignmentUpdate(assignment, dto, currentUser, schedule, scheduleId);
    }

    @Transactional
    public ScheduleAssignmentResponseDto updateAssignmentById(String assignmentId, AssignmentEditRequestDto dto, UserPrincipal currentUser) {
        ScheduleAssignment assignment = scheduleAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule assignment not found with ID: " + assignmentId));

        return applyAssignmentUpdate(assignment, dto, currentUser, assignment.getSchedule(), assignmentId);
    }

    private ScheduleAssignmentResponseDto applyAssignmentUpdate(
            ScheduleAssignment assignment,
            AssignmentEditRequestDto dto,
            UserPrincipal currentUser,
            Schedule schedule,
            String scheduleId) {

        if (dto.getVersion() != null && !dto.getVersion().equals(assignment.getVersion())) {
            throw new ResourceConflictException("Stale write error: Assignment was modified by another user (version mismatch)");
        }

        User modifier = userRepository.findById(currentUser.getId()).orElse(null);

        if (dto.getEmployeeId() != null && !dto.getEmployeeId().isBlank()) {
            Employee emp = employeeRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + dto.getEmployeeId()));

            revalidateManualEdit(assignment, emp, schedule, Boolean.TRUE.equals(dto.getIsOvertime()));

            assignment.setEmployee(emp);
            assignment.setStatus(AssignmentStatus.ASSIGNED);
        } else {
            assignment.setEmployee(null);
            assignment.setStatus(AssignmentStatus.UNFILLED);
        }

        if (dto.getIsOvertime() != null) {
            assignment.setIsOvertime(dto.getIsOvertime());
        }

        assignment.setLastModifiedBy(modifier);
        assignment.setLastModifiedAt(LocalDateTime.now());

        ScheduleAssignment updated = scheduleAssignmentRepository.save(assignment);

        if (schedule != null && schedule.getStatus() == ScheduleStatus.PUBLISHED) {
            notificationService.notifyShiftChanged(updated);
        }

        if (schedule != null) {
            List<ScheduleAssignment> allAssignments = scheduleAssignmentRepository.findByScheduleId(scheduleId);
            boolean hasShortages = allAssignments.stream().anyMatch(a ->
                    a.getStatus() == AssignmentStatus.UNFILLED || a.getStatus() == AssignmentStatus.NEEDS_REASSIGNMENT);
            boolean shortagesResolved = Boolean.TRUE.equals(schedule.getHasShortages()) && !hasShortages;
            schedule.setHasShortages(hasShortages);
            scheduleRepository.save(schedule);

            String event = shortagesResolved ? "SHORTAGE_RESOLVED" : "ASSIGNMENT_UPDATED";
            scheduleRealtimePublisher.publishScheduleChanged(scheduleId, event,
                    "Schedule assignment updated; shortages=" + hasShortages);
        }

        auditService.log("ScheduleAssignment", updated.getId(), AuditAction.UPDATE, null, updated);
        return ScheduleAssignmentResponseDto.fromEntity(updated);
    }

    @Transactional
    public ScheduleResponseDto publishSchedule(String id, SchedulePublishRequestDto dto, UserPrincipal currentUser) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + id));

        if (schedule.getStatus() == ScheduleStatus.PUBLISHED) {
            throw new InvalidOperationException("Schedule is already published");
        }

        if (Boolean.TRUE.equals(schedule.getHasShortages())) {
            if (dto == null || !Boolean.TRUE.equals(dto.getAcknowledgeShortages())) {
                throw new InvalidOperationException("Schedule contains unfilled shortages. Explicit acknowledgeShortages=true flag is required in request to publish.");
            }
        }

        Schedule oldSnapshot = Schedule.builder()
                .id(schedule.getId())
                .status(schedule.getStatus())
                .build();

        schedule.setStatus(ScheduleStatus.PUBLISHED);
        schedule.setPublishedAt(LocalDateTime.now());

        Schedule updated = scheduleRepository.save(schedule);

        auditService.log("Schedule", updated.getId(), AuditAction.APPROVE, oldSnapshot, updated);
        notificationService.notifySchedulePublished(updated);
        scheduleRealtimePublisher.publishScheduleChanged(id, "SCHEDULE_PUBLISHED", "Schedule published");

        return getScheduleById(id);
    }

    private void revalidateManualEdit(ScheduleAssignment assignment, Employee emp, Schedule schedule, boolean overtimeApproved) {
        LocalDate date = assignment.getAssignmentDate();
        ShiftTemplate shift = assignment.getShiftTemplate();

        // 1. Employee Active Status
        if (emp.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new InvalidOperationException("Manual edit rejected: Employee '" + emp.getFirstName() + " " + emp.getLastName() + "' is not ACTIVE");
        }

        // 2. Approved Leave
        List<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeIdAndStatus(emp.getId(), LeaveStatus.APPROVED);
        for (LeaveRequest l : leaves) {
            if (!date.isBefore(l.getStartDate()) && !date.isAfter(l.getEndDate())) {
                throw new InvalidOperationException("Manual edit rejected: Employee '" + emp.getFirstName() + " " + emp.getLastName() +
                        "' is on approved leave on " + date);
            }
        }

        // 3. Unavailability
        List<Availability> avails = availabilityRepository.findByEmployeeIdAndUnavailableDate(emp.getId(), date);
        for (Availability a : avails) {
            if (a.getShiftTemplate() == null || a.getShiftTemplate().getId().equals(shift.getId())) {
                throw new InvalidOperationException("Manual edit rejected: Employee '" + emp.getFirstName() + " " + emp.getLastName() +
                        "' declared unavailability on " + date);
            }
        }

        List<ScheduleAssignment> sameDay = scheduleAssignmentRepository
                .findByEmployeeIdAndAssignmentDate(emp.getId(), date);
        for (ScheduleAssignment existing : sameDay) {
            if (!existing.getId().equals(assignment.getId()) && existing.getStatus() == AssignmentStatus.ASSIGNED) {
                throw new InvalidOperationException("Manual edit rejected: Employee '" + emp.getFirstName() + " " + emp.getLastName() +
                        "' is already assigned on " + date + " (double-booking)");
            }
        }

        // 4. Max Weekly Hours Cumulative Check
        double maxWeeklyHours = emp.getMaxWeeklyHoursOverride() != null
                ? emp.getMaxWeeklyHoursOverride()
                : getComplianceRuleDouble("MAX_WEEKLY_HOURS", 40.0);

        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = date.with(DayOfWeek.SUNDAY);

        List<ScheduleAssignment> currentWeekAssignments = scheduleAssignmentRepository
                .findByEmployeeIdAndAssignmentDateBetween(emp.getId(), weekStart, weekEnd);

        double totalHours = currentWeekAssignments.stream()
                .filter(a -> !a.getId().equals(assignment.getId()))
                .filter(a -> a.getStatus() == AssignmentStatus.ASSIGNED && a.getShiftTemplate() != null)
                .mapToDouble(a -> a.getShiftTemplate().getDurationHours().doubleValue())
                .sum();

        totalHours += shift.getDurationHours().doubleValue();

        if (totalHours > maxWeeklyHours && !overtimeApproved) {
            throw new InvalidOperationException("Manual edit rejected: Cumulative weekly hours (" + totalHours +
                    " hrs) would exceed maximum allowed limit (" + maxWeeklyHours +
                    " hrs). Set isOvertime=true to explicitly approve overtime.");
        }

        // 5. Minimum Rest Hours Check
        double minRestHours = getComplianceRuleDouble("MIN_REST_HOURS", 11.0);
        List<ScheduleAssignment> adjacentAssignments = scheduleAssignmentRepository
                .findByEmployeeIdAndAssignmentDateBetween(emp.getId(), date.minusDays(1), date.plusDays(1));

        LocalDateTime newStart = date.atTime(shift.getStartTime());
        for (ScheduleAssignment a : adjacentAssignments) {
            if (a.getId().equals(assignment.getId()) || a.getStatus() != AssignmentStatus.ASSIGNED || a.getShiftTemplate() == null) {
                continue;
            }

            LocalDateTime aEnd = a.getAssignmentDate().atTime(a.getShiftTemplate().getEndTime());
            if (a.getShiftTemplate().getEndTime().isBefore(a.getShiftTemplate().getStartTime())) {
                aEnd = a.getAssignmentDate().plusDays(1).atTime(a.getShiftTemplate().getEndTime());
            }

            if (!newStart.isBefore(aEnd)) {
                double rest = ChronoUnit.MINUTES.between(aEnd, newStart) / 60.0;
                if (rest < minRestHours) {
                    throw new InvalidOperationException("Manual edit rejected: Minimum rest hours conflict (" +
                            String.format("%.1f", rest) + " hrs rest vs " + minRestHours + " hrs required)");
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getAllSchedules(String departmentId) {
        List<Schedule> schedules;
        if (departmentId != null && !departmentId.isBlank()) {
            schedules = scheduleRepository.findByDepartmentId(departmentId);
        } else {
            schedules = scheduleRepository.findAll();
        }

        return schedules.stream()
                .map(s -> getScheduleById(s.getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScheduleAssignmentResponseDto> getMyAssignments(UserPrincipal principal) {
        if (principal.getEmployeeId() == null) {
            return Collections.emptyList();
        }
        List<ScheduleAssignment> assignments = scheduleAssignmentRepository
                .findMyPublishedAssignments(principal.getEmployeeId());
        return assignments.stream()
                .map(ScheduleAssignmentResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    private double getComplianceRuleDouble(String key, double defaultValue) {
        return complianceRuleRepository.findByRuleKey(key)
                .map(r -> r.getRuleValue().doubleValue())
                .orElse(defaultValue);
    }

    private int getComplianceRuleInt(String key, int defaultValue) {
        return complianceRuleRepository.findByRuleKey(key)
                .map(ComplianceRule::getRuleValue)
                .orElse(defaultValue);
    }
}
