package com.hospital.scheduling.audit.dto;

import com.hospital.scheduling.audit.AuditAction;
import com.hospital.scheduling.audit.AuditLog;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDto {
    private String id;
    private String entityType;
    private String entityId;
    private AuditAction action;
    private String performedByUsername;
    private String performedByRole;
    private String oldValue;
    private String newValue;
    private LocalDateTime performedAt;

    public static AuditLogDto fromEntity(AuditLog entity) {
        return AuditLogDto.builder()
                .id(entity.getId())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .action(entity.getAction())
                .performedByUsername(entity.getPerformedBy() != null ? entity.getPerformedBy().getUsername() : null)
                .performedByRole(entity.getPerformedBy() != null ? entity.getPerformedBy().getRole().name() : null)
                .oldValue(entity.getOldValue())
                .newValue(entity.getNewValue())
                .performedAt(entity.getPerformedAt())
                .build();
    }
}
