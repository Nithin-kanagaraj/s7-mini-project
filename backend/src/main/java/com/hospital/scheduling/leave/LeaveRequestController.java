package com.hospital.scheduling.leave;

import com.hospital.scheduling.common.security.UserPrincipal;
import com.hospital.scheduling.leave.dto.*;
import com.hospital.scheduling.user.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @PostMapping("/my-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveRequestResponseDto> submitMyLeave(
            @Valid @RequestBody LeaveSubmitRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal.getEmployeeId() == null) {
            throw new AccessDeniedException("Authenticated user has no associated employee profile");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(leaveRequestService.submitLeave(principal.getEmployeeId(), dto));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LeaveRequestResponseDto>> getMyLeaveRequests(
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal.getEmployeeId() == null) {
            throw new AccessDeniedException("Authenticated user has no associated employee profile");
        }

        return ResponseEntity.ok(leaveRequestService.getLeaveRequests(null, null, principal.getEmployeeId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','SCHEDULER','DEPT_HEAD')")
    public ResponseEntity<List<LeaveRequestResponseDto>> getAllLeaveRequests(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) String employeeId,
            @AuthenticationPrincipal UserPrincipal principal) {

        String effectiveDepartmentId = departmentId;
        if (principal.getRole() == Role.DEPT_HEAD) {
            if (departmentId != null && !departmentId.equals(principal.getDepartmentId())) {
                throw new AccessDeniedException("Department Heads can only view leave requests for their own department");
            }
            effectiveDepartmentId = principal.getDepartmentId();
        }

        return ResponseEntity.ok(leaveRequestService.getLeaveRequests(effectiveDepartmentId, status, employeeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveRequestResponseDto> getLeaveRequestById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {

        LeaveRequestResponseDto lr = leaveRequestService.getLeaveRequestById(id);

        if (principal.getRole() == Role.WORKER && !lr.getEmployeeId().equals(principal.getEmployeeId())) {
            throw new AccessDeniedException("Workers can only view their own leave requests");
        }
        if (principal.getRole() == Role.DEPT_HEAD && !lr.getDepartmentId().equals(principal.getDepartmentId())) {
            throw new AccessDeniedException("Department Heads can only view leave requests for their own department");
        }

        return ResponseEntity.ok(lr);
    }

    @PutMapping("/{id}/decide")
    @PreAuthorize("hasAnyRole('ADMIN','HR','DEPT_HEAD')")
    public ResponseEntity<LeaveRequestResponseDto> decideLeaveRequest(
            @PathVariable String id,
            @Valid @RequestBody LeaveDecisionRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(leaveRequestService.decideLeaveRequest(id, dto, principal));
    }
}
