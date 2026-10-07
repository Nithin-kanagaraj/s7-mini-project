package com.hospital.scheduling.schedule.engine;

import com.hospital.scheduling.department.Department;
import com.hospital.scheduling.department.DepartmentRepository;
import com.hospital.scheduling.employee.Employee;
import com.hospital.scheduling.employee.EmployeeRepository;
import com.hospital.scheduling.employee.EmployeeType;
import com.hospital.scheduling.employee.EmploymentStatus;
import com.hospital.scheduling.availability.Availability;
import com.hospital.scheduling.availability.AvailabilityRepository;
import com.hospital.scheduling.leave.LeaveRequest;
import com.hospital.scheduling.leave.LeaveRequestRepository;
import com.hospital.scheduling.leave.LeaveStatus;
import com.hospital.scheduling.schedule.ScheduleAssignment;
import com.hospital.scheduling.schedule.ScheduleAssignmentRepository;
import com.hospital.scheduling.schedule.dto.ShortageActionDto;
import com.hospital.scheduling.schedule.dto.ShortageItemReportDto;
import com.hospital.scheduling.shift.ShiftTemplate;
import com.hospital.scheduling.shift.StaffingRequirement;
import com.hospital.scheduling.skill.EmployeeSkill;
import com.hospital.scheduling.skill.EmployeeSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShortageDiagnosticService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AvailabilityRepository availabilityRepository;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;

    public ShortageItemReportDto computeDiagnosticReport(
            StaffingRequirement req,
            int assignedCount,
            int shortageCount,
            double maxWeeklyHoursDefault,
            double minRestHoursDefault) {

        Department dept = req.getDepartment();
        ShiftTemplate shift = req.getShiftTemplate();
        LocalDate date = req.getShiftDate();
        EmployeeType reqType = req.getEmployeeType();
        String reqSkillId = req.getRequiredSkill() != null ? req.getRequiredSkill().getId() : null;
        String reqSkillName = req.getRequiredSkill() != null ? req.getRequiredSkill().getName() : "None";

        List<Employee> deptEmployees = employeeRepository.findByDepartmentIdAndEmploymentStatus(dept.getId(), EmploymentStatus.ACTIVE)
                .stream().filter(e -> e.getEmployeeType() == reqType).collect(Collectors.toList());

        // Count eliminations per constraint
        int countLackSkill = 0;
        int countLeaveOrUnavail = 0;
        int countMaxHours = 0;
        int countRestConflict = 0;

        List<String> overtimeEligibleEmpIds = new ArrayList<>();

        for (Employee e : deptEmployees) {
            // Check skill
            boolean skillOk = true;
            if (reqSkillId != null) {
                skillOk = checkSkillValid(e.getId(), reqSkillId, date);
            }
            if (!skillOk) {
                countLackSkill++;
                continue;
            }

            // Check leave & unavailability
            boolean onLeave = checkOnApprovedLeave(e.getId(), date);
            boolean unavail = checkUnavailable(e.getId(), date, shift.getId());
            if (onLeave || unavail) {
                countLeaveOrUnavail++;
                continue;
            }

            // Check hours
            double maxHours = e.getMaxWeeklyHoursOverride() != null ? e.getMaxWeeklyHoursOverride() : maxWeeklyHoursDefault;
            double currentWeeklyHours = calculateEmployeeWeeklyHours(e.getId(), date);
            if (currentWeeklyHours + shift.getDurationHours().doubleValue() > maxHours) {
                countMaxHours++;
                overtimeEligibleEmpIds.add(e.getId());
                continue;
            }

            // Check rest
            boolean restConflict = checkRestConflict(e.getId(), date, shift, minRestHoursDefault);
            if (restConflict) {
                countRestConflict++;
            }
        }

        // Determine dominant reason
        String reason = "INSUFFICIENT_QUALIFIED_STAFF";
        int maxEliminated = countLackSkill;

        if (countLeaveOrUnavail > maxEliminated) {
            reason = "INSUFFICIENT_AVAILABLE_STAFF";
            maxEliminated = countLeaveOrUnavail;
        }
        if (countMaxHours > maxEliminated) {
            reason = "MAX_HOURS_EXHAUSTED";
            maxEliminated = countMaxHours;
        }
        if (countRestConflict > maxEliminated) {
            reason = "REST_PERIOD_CONFLICT";
        }

        String detail = String.format(
                "Shortage of %d slot(s) for %s (%s) on %s in %s. Diagnostics on %d active dept staff: " +
                "%d lacked required skill '%s', %d on leave/unavailable, %d exhausted weekly hours limit, %d had rest window conflict.",
                shortageCount, reqType, shift.getName(), date, dept.getName(), deptEmployees.size(),
                countLackSkill, reqSkillName, countLeaveOrUnavail, countMaxHours, countRestConflict
        );

        // Compute Candidate Departments for Cross-Department Transfer
        List<String> candidateDepts = new ArrayList<>();
        for (Department otherDept : departmentRepository.findAll()) {
            if (!otherDept.getId().equals(dept.getId()) && Boolean.TRUE.equals(otherDept.getIsActive())) {
                long qualifiedCount = employeeRepository.findByDepartmentIdAndEmploymentStatus(otherDept.getId(), EmploymentStatus.ACTIVE)
                        .stream()
                        .filter(e -> e.getEmployeeType() == reqType)
                        .filter(e -> reqSkillId == null || checkSkillValid(e.getId(), reqSkillId, date))
                        .filter(e -> !checkOnApprovedLeave(e.getId(), date))
                        .count();
                if (qualifiedCount > 0) {
                    candidateDepts.add(otherDept.getName());
                }
            }
        }

        List<ShortageActionDto> actions = new ArrayList<>();
        actions.add(ShortageActionDto.builder()
                .action("ASSIGN_OVERTIME")
                .eligibleEmployees(overtimeEligibleEmpIds)
                .note("Waive weekly max hours limit for overtime assignment")
                .build());

        actions.add(ShortageActionDto.builder()
                .action("CROSS_DEPARTMENT_TRANSFER")
                .candidateDepartments(candidateDepts)
                .note("Transfer qualified staff from available departments")
                .build());

        actions.add(ShortageActionDto.builder()
                .action("REDUCE_REQUIREMENT")
                .note("Requires Department Head approval, logged in audit trail")
                .build());

        actions.add(ShortageActionDto.builder()
                .action("MANUAL_OVERRIDE")
                .note("Manual administrative override")
                .build());

        return ShortageItemReportDto.builder()
                .department(dept.getName())
                .date(date.toString())
                .shift(shift.getName())
                .required(req.getRequiredCount())
                .assigned(assignedCount)
                .shortage(shortageCount)
                .reason(reason)
                .detail(detail)
                .possibleActions(actions)
                .build();
    }

    private boolean checkSkillValid(String employeeId, String skillId, LocalDate date) {
        List<EmployeeSkill> skills = employeeSkillRepository.findByEmployeeId(employeeId);
        for (EmployeeSkill es : skills) {
            if (es.getSkill().getId().equals(skillId)) {
                boolean certified = es.getCertifiedDate() == null || !es.getCertifiedDate().isAfter(date);
                boolean notExpired = es.getExpiryDate() == null || !es.getExpiryDate().isBefore(date);
                if (certified && notExpired) return true;
            }
        }
        return false;
    }

    private boolean checkOnApprovedLeave(String employeeId, LocalDate date) {
        List<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeIdAndStatus(employeeId, LeaveStatus.APPROVED);
        for (LeaveRequest lr : leaves) {
            if (!date.isBefore(lr.getStartDate()) && !date.isAfter(lr.getEndDate())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkUnavailable(String employeeId, LocalDate date, String shiftId) {
        List<Availability> avails = availabilityRepository.findByEmployeeIdAndUnavailableDate(employeeId, date);
        for (Availability a : avails) {
            if (a.getShiftTemplate() == null || a.getShiftTemplate().getId().equals(shiftId)) {
                return true;
            }
        }
        return false;
    }

    private double calculateEmployeeWeeklyHours(String employeeId, LocalDate date) {
        LocalDate weekStart = date.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = date.with(java.time.DayOfWeek.SUNDAY);
        List<ScheduleAssignment> assignments = scheduleAssignmentRepository.findByEmployeeIdAndAssignmentDateBetween(employeeId, weekStart, weekEnd);
        return assignments.stream()
                .filter(a -> a.getShiftTemplate() != null)
                .mapToDouble(a -> a.getShiftTemplate().getDurationHours().doubleValue())
                .sum();
    }

    private boolean checkRestConflict(String employeeId, LocalDate date, ShiftTemplate shift, double minRestHours) {
        List<ScheduleAssignment> adjacent = scheduleAssignmentRepository.findByEmployeeIdAndAssignmentDateBetween(employeeId, date.minusDays(1), date.plusDays(1));
        LocalDateTime newStart = date.atTime(shift.getStartTime());

        for (ScheduleAssignment a : adjacent) {
            if (a.getShiftTemplate() == null) continue;
            LocalDateTime aEnd = a.getAssignmentDate().atTime(a.getShiftTemplate().getEndTime());
            if (a.getShiftTemplate().getEndTime().isBefore(a.getShiftTemplate().getStartTime())) {
                aEnd = a.getAssignmentDate().plusDays(1).atTime(a.getShiftTemplate().getEndTime());
            }

            if (!newStart.isBefore(aEnd)) {
                double rest = ChronoUnit.MINUTES.between(aEnd, newStart) / 60.0;
                if (rest < minRestHours) return true;
            }
        }
        return false;
    }
}
