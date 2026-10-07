package com.hospital.scheduling.skill.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSkillAssignDto {

    @NotBlank(message = "Skill ID is required")
    private String skillId;

    private LocalDate certifiedDate;
    private LocalDate expiryDate;
}
