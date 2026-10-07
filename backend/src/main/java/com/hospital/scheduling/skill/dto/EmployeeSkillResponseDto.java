package com.hospital.scheduling.skill.dto;

import com.hospital.scheduling.skill.EmployeeSkill;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSkillResponseDto {
    private String employeeId;
    private String skillId;
    private String skillName;
    private String skillDescription;
    private LocalDate certifiedDate;
    private LocalDate expiryDate;
    private boolean isExpired;

    public static EmployeeSkillResponseDto fromEntity(EmployeeSkill es) {
        boolean expired = es.getExpiryDate() != null && es.getExpiryDate().isBefore(LocalDate.now());
        return EmployeeSkillResponseDto.builder()
                .employeeId(es.getId().getEmployeeId())
                .skillId(es.getId().getSkillId())
                .skillName(es.getSkill() != null ? es.getSkill().getName() : null)
                .skillDescription(es.getSkill() != null ? es.getSkill().getDescription() : null)
                .certifiedDate(es.getCertifiedDate())
                .expiryDate(es.getExpiryDate())
                .isExpired(expired)
                .build();
    }
}
