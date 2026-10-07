package com.hospital.scheduling.leave;

import com.hospital.scheduling.audit.AuditAction;
import com.hospital.scheduling.audit.AuditService;
import com.hospital.scheduling.common.exception.InvalidOperationException;
import com.hospital.scheduling.common.exception.ResourceNotFoundException;
import com.hospital.scheduling.common.security.UserPrincipal;
import com.hospital.scheduling.employee.Employee;
import com.hospital.scheduling.employee.EmployeeRepository;
import com.hospital.scheduling.leave.dto.*;
import com.hospital.scheduling.user.Role;
import com.hospital.scheduling.user.User;
import com.hospital.scheduling.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final com.hospital.scheduling.schedule.ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final com.hospital.scheduling.schedule.ScheduleRepository scheduleRepository;
    private final com.hospital.scheduling.notification.NotificationService notificationService;
    private final com.hospital.scheduling.schedule.ScheduleRealtimePublisher scheduleRealtimePublisher;
    private final AuditService auditService;

    @Transactional
    public LeaveRequestResponseDto submitLeave(String employeeId, LeaveSubmitRequestDto dto) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new InvalidOperationException("Leave end date (" + dto.getEndDate() + ") cannot be before start date (" + dto.getStartDate() + ")");
        }

        LocalDate today = LocalDate.now();
        boolean isRetroactive = dto.getStartDate().isBefore(today);
        if (isRetroactive && (dto.getReason() == null || dto.getReason().trim().isEmpty())) {
            throw new InvalidOperationException("Retroactive leave request (start date in the past) requires an explicit reason");
        }

        // Check for overlapping PENDING/APPROVED leaves for this employee
        List<LeaveRequest> overlaps = leaveRequestRepository.findOverlappingLeaves(
                employeeId, dto.getStartDate(), dto.getEndDate(), Arrays.asList(LeaveStatus.PENDING, LeaveStatus.APPROVED));

        boolean hasOverlapWarning = !overlaps.isEmpty();
        String overlapWarningMessage = null;
        if (hasOverlapWarning) {
            overlapWarningMessage = "Warning: Employee already has " + overlaps.size() +
                    " overlapping PENDING/APPROVED leave request(s) during this date range.";
        }

        LeaveRequest leave = LeaveRequest.builder()
                .employee(emp)
                .leaveType(dto.getLeaveType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(LeaveStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        LeaveRequest saved = leaveRequestRepository.save(leave);
        auditService.log("LeaveRequest", saved.getId(), AuditAction.CREATE, null, saved);

        return LeaveRequestResponseDto.fromEntity(saved, hasOverlapWarning, overlapWarningMessage);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDto> getLeaveRequests(String departmentId, LeaveStatus status, String employeeId) {
        List<LeaveRequest> list;
        if (employeeId != null) {
            list = leaveRequestRepository.findByEmployeeId(employeeId);
        } else if (departmentId != null && status != null) {
            list = leaveRequestRepository.findByEmployeeDepartmentIdAndStatus(departmentId, status);
        } else if (departmentId != null) {
            list = leaveRequestRepository.findByEmployeeDepartmentId(departmentId);
        } else if (status != null) {
            list = leaveRequestRepository.findByStatus(status);
        } else {
            list = leaveRequestRepository.findAll();
        }

        return list.stream()
                .map(LeaveRequestResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LeaveRequestResponseDto getLeaveRequestById(String id) {
        LeaveRequest lr = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + id));
        return LeaveRequestResponseDto.fromEntity(lr);
    }

    @Transactional
    public LeaveRequestResponseDto decideLeaveRequest(String leaveRequestId, LeaveDecisionRequestDto dto, UserPrincipal currentApprover) {
        LeaveRequest lr = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + leaveRequestId));

        if (lr.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidOperationException("Leave request has already been decided (current status: " + lr.getStatus() + ")");
        }

        // Server-side department scoping for DEPT_HEAD
        if (currentApprover.getRole() == Role.DEPT_HEAD) {
            String empDeptId = lr.getEmployee() != null && lr.getEmployee().getDepartment() != null
                    ? lr.getEmployee().getDepartment().getId() : null;
            if (empDeptId == null || !empDeptId.equals(currentApprover.getDepartmentId())) {
                throw new AccessDeniedException("Department Heads can only approve or reject leave requests for employees in their own department");
            }
        }

        User approverUser = userRepository.findById(currentApprover.getId()).orElse(null);

        LeaveRequest oldSnapshot = LeaveRequest.builder()
                .id(lr.getId())
                .status(lr.getStatus())
                .build();

        lr.setStatus(dto.getStatus());
        lr.setApprovedBy(approverUser);
        lr.setDecidedAt(LocalDateTime.now());

        LeaveRequest updated = leaveRequestRepository.save(lr);

        AuditAction action = (dto.getStatus() == LeaveStatus.APPROVED) ? AuditAction.APPROVE : AuditAction.UPDATE;
        auditService.log("LeaveRequest", updated.getId(), action, oldSnapshot, updated);

        // Notify employee
        notificationService.notifyLeaveDecision(updated);

        // Check if approval conflicts with published schedule
        if (dto.getStatus() == LeaveStatus.APPROVED && updated.getEmployee() != null) {
            List<com.hospital.scheduling.schedule.ScheduleAssignment> publishedAssignments =
                    scheduleAssignmentRepository.findPublishedAssignmentsForEmployeeInRange(
                            updated.getEmployee().getId(), updated.getStartDate(), updated.getEndDate());

            if (!publishedAssignments.isEmpty()) {
                java.util.Set<String> scheduleIds = new java.util.HashSet<>();
                for (com.hospital.scheduling.schedule.ScheduleAssignment sa : publishedAssignments) {
                    sa.setStatus(com.hospital.scheduling.schedule.AssignmentStatus.NEEDS_REASSIGNMENT);
                    sa.setEmployee(null);
                    scheduleAssignmentRepository.save(sa);
                    if (sa.getSchedule() != null) {
                        scheduleIds.add(sa.getSchedule().getId());
                    }
                }
                for (String scheduleId : scheduleIds) {
                    scheduleRepository.findById(scheduleId).ifPresent(schedule -> {
                        schedule.setHasShortages(true);
                        scheduleRepository.save(schedule);
                        scheduleRealtimePublisher.publishScheduleChanged(scheduleId, "SHORTAGE_APPEARED",
                                "Leave approval created NEEDS_REASSIGNMENT slots on a published schedule");
                    });
                }
                notificationService.notifyLeaveConflict(updated, publishedAssignments);
            }
        }

        return LeaveRequestResponseDto.fromEntity(updated);
    }
}
