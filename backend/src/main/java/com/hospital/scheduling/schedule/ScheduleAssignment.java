package com.hospital.scheduling.schedule;

import com.hospital.scheduling.department.Department;
import com.hospital.scheduling.employee.Employee;
import com.hospital.scheduling.shift.ShiftTemplate;
import com.hospital.scheduling.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_assignments")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class ScheduleAssignment {

    public ScheduleAssignment() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    @Id
    @Column(columnDefinition = "CHAR(36)", length = 36)
    private String id;

    public void setId(String id) {
        this.id = (id != null) ? id : java.util.UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = true)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    public void setEmployee(Employee employee) {
        this.employee = employee;
        if (this.department == null && employee != null && employee.getDepartment() != null) {
            this.department = employee.getDepartment();
        }
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_template_id", nullable = false)
    private ShiftTemplate shiftTemplate;

    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    @Column(name = "is_overtime", nullable = false)
    @Builder.Default
    private Boolean isOvertime = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by")
    private User lastModifiedBy;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }

    public void setStatus(String status) {
        if (status == null || status.isBlank()) {
            this.status = AssignmentStatus.ASSIGNED;
            return;
        }
        this.status = AssignmentStatus.valueOf(status.trim().toUpperCase());
    }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (department == null && employee != null && employee.getDepartment() != null) {
            department = employee.getDepartment();
        }
    }
}
