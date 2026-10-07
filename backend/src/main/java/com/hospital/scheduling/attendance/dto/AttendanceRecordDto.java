package com.hospital.scheduling.attendance.dto;

import com.hospital.scheduling.attendance.AttendanceRecord;
import com.hospital.scheduling.attendance.EntryMethod;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecordDto {
    private String id;
    private String assignmentId;
    private String employeeId;
    private String employeeName;
    private String shiftTemplateName;
    private String assignmentDate;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private Double actualHours;
    private Double plannedHours;
    private EntryMethod entryMethod;
    private String recordedByUsername;
    private Double weeklyCountableHours;
    private Double maxWeeklyHours;
    private Double overtimeHoursDetected;
    private Boolean overtimeApproved;
    private Boolean overtimeApprovalRequired;

    public static AttendanceRecordDto fromEntity(AttendanceRecord entity) {
        Double actual = null;
        if (entity.getClockIn() != null && entity.getClockOut() != null) {
            actual = java.time.Duration.between(entity.getClockIn(), entity.getClockOut()).toMinutes() / 60.0;
        }

        Double planned = null;
        if (entity.getAssignment() != null && entity.getAssignment().getShiftTemplate() != null) {
            planned = entity.getAssignment().getShiftTemplate().getDurationHours().doubleValue();
        }

        return AttendanceRecordDto.builder()
                .id(entity.getId())
                .assignmentId(entity.getAssignment() != null ? entity.getAssignment().getId() : null)
                .employeeId(entity.getAssignment() != null && entity.getAssignment().getEmployee() != null ? entity.getAssignment().getEmployee().getId() : null)
                .employeeName(entity.getAssignment() != null && entity.getAssignment().getEmployee() != null ? entity.getAssignment().getEmployee().getFirstName() + " " + entity.getAssignment().getEmployee().getLastName() : null)
                .shiftTemplateName(entity.getAssignment() != null && entity.getAssignment().getShiftTemplate() != null ? entity.getAssignment().getShiftTemplate().getName() : null)
                .assignmentDate(entity.getAssignment() != null && entity.getAssignment().getAssignmentDate() != null ? entity.getAssignment().getAssignmentDate().toString() : null)
                .clockIn(entity.getClockIn())
                .clockOut(entity.getClockOut())
                .actualHours(actual)
                .plannedHours(planned)
                .entryMethod(entity.getEntryMethod())
                .recordedByUsername(entity.getRecordedBy() != null ? entity.getRecordedBy().getUsername() : null)
                .build();
    }
}
