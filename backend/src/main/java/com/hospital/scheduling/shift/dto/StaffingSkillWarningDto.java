package com.hospital.scheduling.shift.dto;

import com.hospital.scheduling.employee.EmployeeType;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffingSkillWarningDto {
    private String requirementId;
    private String departmentId;
    private String departmentName;
    private LocalDate shiftDate;
    private String shiftTemplateName;
    private EmployeeType employeeType;
    private String skillId;
    private String skillName;
    private String warningMessage;
    private long activeQualifiedEmployeeCount;
}
