package com.hospital.scheduling.employee.dto;

import com.hospital.scheduling.employee.Employee;
import com.hospital.scheduling.employee.EmployeeType;
import com.hospital.scheduling.employee.EmploymentStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponseDto {
    private String id;
    private String firstName;
    private String lastName;
    private String fullName;
    private EmployeeType employeeType;
    private String departmentId;
    private String departmentName;
    private String contactEmail;
    private String contactPhone;
    private EmploymentStatus employmentStatus;
    private LocalDate hireDate;
    private Integer maxWeeklyHoursOverride;
    private Integer version;
    private LocalDateTime createdAt;

    public static EmployeeResponseDto fromEntity(Employee emp) {
        return EmployeeResponseDto.builder()
                .id(emp.getId())
                .firstName(emp.getFirstName())
                .lastName(emp.getLastName())
                .fullName(emp.getFirstName() + " " + emp.getLastName())
                .employeeType(emp.getEmployeeType())
                .departmentId(emp.getDepartment() != null ? emp.getDepartment().getId() : null)
                .departmentName(emp.getDepartment() != null ? emp.getDepartment().getName() : null)
                .contactEmail(emp.getContactEmail())
                .contactPhone(emp.getContactPhone())
                .employmentStatus(emp.getEmploymentStatus())
                .hireDate(emp.getHireDate())
                .maxWeeklyHoursOverride(emp.getMaxWeeklyHoursOverride())
                .version(emp.getVersion())
                .createdAt(emp.getCreatedAt())
                .build();
    }
}
