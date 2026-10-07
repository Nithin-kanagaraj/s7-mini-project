package com.hospital.scheduling.shift.dto;

import com.hospital.scheduling.employee.EmployeeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffingRequirementRequestDto {

    @NotBlank(message = "Department ID is required")
    private String departmentId;

    @NotBlank(message = "Shift template ID is required")
    private String shiftTemplateId;

    @NotNull(message = "Shift date is required")
    private LocalDate shiftDate;

    @NotNull(message = "Employee type is required")
    private EmployeeType employeeType;

    private String requiredSkillId;

    @NotNull(message = "Required count is required")
    @Min(value = 0, message = "Required count must be 0 or greater")
    private Integer requiredCount;
}
