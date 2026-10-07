package com.hospital.scheduling.shift;

import com.hospital.scheduling.department.Department;
import com.hospital.scheduling.employee.EmployeeType;
import com.hospital.scheduling.skill.Skill;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "staffing_requirements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffingRequirement {

    @Id
    @Column(columnDefinition = "CHAR(36)", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_template_id", nullable = false)
    private ShiftTemplate shiftTemplate;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "employee_type", nullable = false, length = 30)
    private EmployeeType employeeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_skill_id")
    private Skill requiredSkill;

    @Column(name = "required_count", nullable = false)
    private Integer requiredCount;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
    }
}
