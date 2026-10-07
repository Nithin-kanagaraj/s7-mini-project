package com.hospital.scheduling.leave.dto;

import com.hospital.scheduling.leave.LeaveRequest;
import com.hospital.scheduling.leave.LeaveStatus;
import com.hospital.scheduling.leave.LeaveType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestResponseDto {
    private String id;
    private String employeeId;
    private String employeeName;
    private String departmentId;
    private String departmentName;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LeaveStatus status;
    private String approvedById;
    private String approvedByName;
    private LocalDateTime requestedAt;
    private LocalDateTime decidedAt;
    private boolean isRetroactive;
    private boolean hasOverlapWarning;
    private String overlapWarningMessage;

    public static LeaveRequestResponseDto fromEntity(LeaveRequest lr) {
        return fromEntity(lr, false, null);
    }

    public static LeaveRequestResponseDto fromEntity(LeaveRequest lr, boolean hasOverlapWarning, String overlapWarningMessage) {
        boolean retroactive = lr.getStartDate() != null && lr.getRequestedAt() != null &&
                lr.getStartDate().isBefore(lr.getRequestedAt().toLocalDate());

        String empName = null;
        String deptId = null;
        String deptName = null;
        if (lr.getEmployee() != null) {
            empName = lr.getEmployee().getFirstName() + " " + lr.getEmployee().getLastName();
            if (lr.getEmployee().getDepartment() != null) {
                deptId = lr.getEmployee().getDepartment().getId();
                deptName = lr.getEmployee().getDepartment().getName();
            }
        }

        String approverName = null;
        if (lr.getApprovedBy() != null) {
            approverName = lr.getApprovedBy().getUsername();
        }

        return LeaveRequestResponseDto.builder()
                .id(lr.getId())
                .employeeId(lr.getEmployee() != null ? lr.getEmployee().getId() : null)
                .employeeName(empName)
                .departmentId(deptId)
                .departmentName(deptName)
                .leaveType(lr.getLeaveType())
                .startDate(lr.getStartDate())
                .endDate(lr.getEndDate())
                .status(lr.getStatus())
                .approvedById(lr.getApprovedBy() != null ? lr.getApprovedBy().getId() : null)
                .approvedByName(approverName)
                .requestedAt(lr.getRequestedAt())
                .decidedAt(lr.getDecidedAt())
                .isRetroactive(retroactive)
                .hasOverlapWarning(hasOverlapWarning)
                .overlapWarningMessage(overlapWarningMessage)
                .build();
    }
}
