package com.hospital.scheduling.skill;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EmployeeSkillId implements Serializable {

    @Column(name = "employee_id", columnDefinition = "CHAR(36)", length = 36)
    private String employeeId;

    @Column(name = "skill_id", columnDefinition = "CHAR(36)", length = 36)
    private String skillId;
}
