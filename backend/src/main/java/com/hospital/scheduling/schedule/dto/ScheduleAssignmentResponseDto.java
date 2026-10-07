package com.hospital.scheduling.schedule.dto;

import com.hospital.scheduling.schedule.AssignmentStatus;
import com.hospital.scheduling.schedule.ScheduleAssignment;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleAssignmentResponseDto {
    private String id;
    private String scheduleId;
    private String employeeId;
    private String employeeName;
    private String departmentId;
    private String departmentName;
    private String shiftTemplateId;
    private String shiftTemplateName;
    private LocalTime shiftStartTime;
    private LocalTime shiftEndTime;
    private LocalDate assignmentDate;
    private AssignmentStatus status;
    private Boolean isOvertime;
    private Integer version;

    public static ScheduleAssignmentResponseDto fromEntity(ScheduleAssignment sa) {
        String empName = null;
        if (sa.getEmployee() != null) {
            empName = sa.getEmployee().getFirstName() + " " + sa.getEmployee().getLastName();
        }

        return ScheduleAssignmentResponseDto.builder()
                .id(sa.getId())
                .scheduleId(sa.getSchedule() != null ? sa.getSchedule().getId() : null)
                .employeeId(sa.getEmployee() != null ? sa.getEmployee().getId() : null)
                .employeeName(empName)
                .departmentId(sa.getDepartment() != null ? sa.getDepartment().getId() : null)
                .departmentName(sa.getDepartment() != null ? sa.getDepartment().getName() : null)
                .shiftTemplateId(sa.getShiftTemplate() != null ? sa.getShiftTemplate().getId() : null)
                .shiftTemplateName(sa.getShiftTemplate() != null ? sa.getShiftTemplate().getName() : null)
                .shiftStartTime(sa.getShiftTemplate() != null ? sa.getShiftTemplate().getStartTime() : null)
                .shiftEndTime(sa.getShiftTemplate() != null ? sa.getShiftTemplate().getEndTime() : null)
                .assignmentDate(sa.getAssignmentDate())
                .status(sa.getStatus())
                .isOvertime(sa.getIsOvertime())
                .version(sa.getVersion())
                .build();
    }
}
