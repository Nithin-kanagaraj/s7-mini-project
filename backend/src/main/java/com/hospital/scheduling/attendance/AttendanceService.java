package com.hospital.scheduling.attendance;

import com.hospital.scheduling.attendance.dto.AttendanceClockInRequestDto;
import com.hospital.scheduling.attendance.dto.AttendanceRecordDto;
import com.hospital.scheduling.audit.AuditAction;
import com.hospital.scheduling.audit.AuditService;
import com.hospital.scheduling.common.exception.InvalidOperationException;
import com.hospital.scheduling.common.exception.ResourceNotFoundException;
import com.hospital.scheduling.common.security.UserPrincipal;
import com.hospital.scheduling.employee.Employee;
import com.hospital.scheduling.schedule.AssignmentStatus;
import com.hospital.scheduling.schedule.ScheduleAssignment;
import com.hospital.scheduling.schedule.ScheduleAssignmentRepository;
import com.hospital.scheduling.user.Role;
import com.hospital.scheduling.user.User;
import com.hospital.scheduling.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final OvertimeHoursCalculator overtimeHoursCalculator;

    @Transactional
    public AttendanceRecordDto recordAttendance(
            String assignmentId,
            AttendanceClockInRequestDto dto,
            UserPrincipal recorderPrincipal) {

        ScheduleAssignment assignment = scheduleAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule assignment not found with ID: " + assignmentId));

        if (assignment.getStatus() != AssignmentStatus.ASSIGNED || assignment.getEmployee() == null) {
            throw new InvalidOperationException("Cannot record attendance for unassigned or cancelled shift assignment");
        }

        if (recorderPrincipal.getRole() == Role.DEPT_HEAD) {
            if (recorderPrincipal.getDepartmentId() == null
                    || assignment.getDepartment() == null
                    || !recorderPrincipal.getDepartmentId().equals(assignment.getDepartment().getId())) {
                throw new AccessDeniedException("Department Head can only record attendance for their own department");
            }
        }

        LocalDateTime clockOut = dto.getClockOut();
        if (clockOut.isBefore(dto.getClockIn())) {
            clockOut = clockOut.plusDays(1);
        }

        User recorder = userRepository.findById(recorderPrincipal.getId()).orElse(null);

        List<AttendanceRecord> existing = attendanceRecordRepository.findByAssignmentId(assignmentId);
        AttendanceRecord record;
        if (!existing.isEmpty()) {
            record = existing.get(0);
            record.setClockIn(dto.getClockIn());
            record.setClockOut(clockOut);
            record.setRecordedBy(recorder);
            record.setEntryMethod(EntryMethod.MANUAL);
        } else {
            record = AttendanceRecord.builder()
                    .assignment(assignment)
                    .clockIn(dto.getClockIn())
                    .clockOut(clockOut)
                    .entryMethod(EntryMethod.MANUAL)
                    .recordedBy(recorder)
                    .build();
        }

        AttendanceRecord saved = attendanceRecordRepository.save(record);

        WeeklyOvertimeSnapshot snapshot = applyWeeklyOvertimeFlags(
                assignment.getEmployee(),
                assignment.getAssignmentDate(),
                Boolean.TRUE.equals(dto.getApproveOvertime()));

        auditService.log("AttendanceRecord", saved.getId(), AuditAction.CREATE, null, saved);

        AttendanceRecordDto result = AttendanceRecordDto.fromEntity(saved);
        result.setWeeklyCountableHours(snapshot.weeklyHours());
        result.setMaxWeeklyHours(snapshot.maxWeeklyHours());
        result.setOvertimeHoursDetected(snapshot.overtimeHours());
        result.setOvertimeApproved(Boolean.TRUE.equals(assignment.getIsOvertime()) || Boolean.TRUE.equals(dto.getApproveOvertime()));
        result.setOvertimeApprovalRequired(snapshot.overtimeHours() > 0 && !Boolean.TRUE.equals(result.getOvertimeApproved()));
        return result;
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> getAttendanceByAssignment(String assignmentId) {
        return attendanceRecordRepository.findByAssignmentId(assignmentId).stream()
                .map(AttendanceRecordDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> getAllAttendance() {
        return attendanceRecordRepository.findAll().stream()
                .map(AttendanceRecordDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Recalculates weekly countable hours (actual when present, else planned).
     * Does not silently approve overtime: {@code is_overtime} is set only when
     * the assignment was already approved or {@code approveOvertime} is true.
     */
    private WeeklyOvertimeSnapshot applyWeeklyOvertimeFlags(Employee employee, LocalDate assignmentDate, boolean approveOvertime) {
        LocalDate weekStart = assignmentDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = assignmentDate.with(DayOfWeek.SUNDAY);

        List<ScheduleAssignment> weekAssignments = scheduleAssignmentRepository
                .findByEmployeeIdAndAssignmentDateBetween(employee.getId(), weekStart, weekEnd);

        double maxWeeklyHours = overtimeHoursCalculator.maxWeeklyHours(employee);
        double runningHours = 0.0;

        for (ScheduleAssignment sa : weekAssignments) {
            if (!overtimeHoursCalculator.isCountableAssignment(sa)) {
                continue;
            }
            List<AttendanceRecord> attendance = attendanceRecordRepository.findByAssignmentId(sa.getId());
            runningHours += overtimeHoursCalculator.hoursFor(sa, attendance).getCountableHours();
        }

        double overtimeHours = overtimeHoursCalculator.overtimeBeyondCap(runningHours, maxWeeklyHours);

        if (approveOvertime && overtimeHours > 0) {
            double remainingCap = maxWeeklyHours;
            for (ScheduleAssignment sa : weekAssignments) {
                if (!overtimeHoursCalculator.isCountableAssignment(sa)) {
                    continue;
                }
                List<AttendanceRecord> attendance = attendanceRecordRepository.findByAssignmentId(sa.getId());
                double hours = overtimeHoursCalculator.hoursFor(sa, attendance).getCountableHours();
                remainingCap -= hours;
                if (remainingCap < 0 && !Boolean.TRUE.equals(sa.getIsOvertime())) {
                    sa.setIsOvertime(true);
                    scheduleAssignmentRepository.save(sa);
                }
            }
        }

        return new WeeklyOvertimeSnapshot(runningHours, maxWeeklyHours, overtimeHours);
    }

    private record WeeklyOvertimeSnapshot(double weeklyHours, double maxWeeklyHours, double overtimeHours) {
    }
}
