package com.hospital.scheduling.shift.dto;

import com.hospital.scheduling.employee.EmployeeType;
import com.hospital.scheduling.shift.StaffingRequirement;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffingRequirementResponseDto {
    private String id;
    private String departmentId;
    private String departmentName;
    private String shiftTemplateId;
    private String shiftTemplateName;
    private LocalTime shiftStartTime;
    private LocalTime shiftEndTime;
    private LocalDate shiftDate;
    private EmployeeType employeeType;
    private String requiredSkillId;
    private String requiredSkillName;
    private Integer requiredCount;
    private boolean isExplicitZero;

    public static StaffingRequirementResponseDto fromEntity(StaffingRequirement sr) {
        boolean zero = sr.getRequiredCount() != null && sr.getRequiredCount() == 0;
        return StaffingRequirementResponseDto.builder()
                .id(sr.getId())
                .departmentId(sr.getDepartment() != null ? sr.getDepartment().getId() : null)
                .departmentName(sr.getDepartment() != null ? sr.getDepartment().getName() : null)
                .shiftTemplateId(sr.getShiftTemplate() != null ? sr.getShiftTemplate().getId() : null)
                .shiftTemplateName(sr.getShiftTemplate() != null ? sr.getShiftTemplate().getName() : null)
                .shiftStartTime(sr.getShiftTemplate() != null ? sr.getShiftTemplate().getStartTime() : null)
                .shiftEndTime(sr.getShiftTemplate() != null ? sr.getShiftTemplate().getEndTime() : null)
                .shiftDate(sr.getShiftDate())
                .employeeType(sr.getEmployeeType())
                .requiredSkillId(sr.getRequiredSkill() != null ? sr.getRequiredSkill().getId() : null)
                .requiredSkillName(sr.getRequiredSkill() != null ? sr.getRequiredSkill().getName() : null)
                .requiredCount(sr.getRequiredCount())
                .isExplicitZero(zero)
                .build();
    }
}
