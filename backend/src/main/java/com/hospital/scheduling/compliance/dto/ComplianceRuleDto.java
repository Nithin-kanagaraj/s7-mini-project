package com.hospital.scheduling.compliance.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceRuleDto {
    private String id;
    private String ruleKey;
    private Integer ruleValue;
    private String description;
    private String updatedByUsername;
    private LocalDateTime updatedAt;

    public static ComplianceRuleDto fromEntity(com.hospital.scheduling.compliance.ComplianceRule entity) {
        return ComplianceRuleDto.builder()
                .id(entity.getId())
                .ruleKey(entity.getRuleKey())
                .ruleValue(entity.getRuleValue())
                .description(entity.getDescription())
                .updatedByUsername(entity.getUpdatedBy() != null ? entity.getUpdatedBy().getUsername() : null)
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
