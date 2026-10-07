package com.hospital.scheduling.skill.dto;

import com.hospital.scheduling.skill.Skill;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillResponseDto {
    private String id;
    private String name;
    private String description;

    public static SkillResponseDto fromEntity(Skill skill) {
        return SkillResponseDto.builder()
                .id(skill.getId())
                .name(skill.getName())
                .description(skill.getDescription())
                .build();
    }
}
