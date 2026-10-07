package com.hospital.scheduling.skill;

import com.hospital.scheduling.employee.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "employee_skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSkill {

    @EmbeddedId
    private EmployeeSkillId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("employeeId")
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(name = "certified_date")
    private LocalDate certifiedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}
