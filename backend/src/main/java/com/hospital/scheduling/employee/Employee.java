package com.hospital.scheduling.employee;

import com.hospital.scheduling.department.Department;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @Column(columnDefinition = "CHAR(36)", length = 36)
    private String id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "employee_type", nullable = false, length = 30)
    private EmployeeType employeeType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "contact_email", nullable = false, unique = true, length = 150)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 20)
    @Builder.Default
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "max_weekly_hours_override")
    private Integer maxWeeklyHoursOverride;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public String getEmployeeId() {
        return this.id;
    }

    public void setEmployeeId(String employeeId) {
        this.id = employeeId;
    }

    public String getEmail() {
        return this.contactEmail;
    }

    public void setEmail(String email) {
        this.contactEmail = email;
    }

    public String getEmploymentType() {
        return this.employeeType != null ? this.employeeType.name() : null;
    }

    public void setEmploymentType(String employmentType) {
        if (employmentType == null || employmentType.isBlank()) {
            return;
        }
        String normalized = employmentType.trim().toUpperCase();
        if ("FULL_TIME".equals(normalized)) {
            normalized = "NURSE";
        }
        this.employeeType = EmployeeType.valueOf(normalized);
    }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (hireDate == null) {
            hireDate = LocalDate.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
