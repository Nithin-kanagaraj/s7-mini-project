package com.hospital.scheduling.employee.dto;

import com.hospital.scheduling.employee.EmployeeType;
import com.hospital.scheduling.employee.EmploymentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeRequestDto {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @NotNull(message = "Employee type is required")
    private EmployeeType employeeType;

    @NotBlank(message = "Department ID is required")
    private String departmentId;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email format")
    @Size(max = 150, message = "Contact email must not exceed 150 characters")
    private String contactEmail;

    private String contactPhone;

    private EmploymentStatus employmentStatus;

    @NotNull(message = "Hire date is required")
    private LocalDate hireDate;

    private Integer maxWeeklyHoursOverride;
}
