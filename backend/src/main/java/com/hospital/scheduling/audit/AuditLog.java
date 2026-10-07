package com.hospital.scheduling.audit;

import com.hospital.scheduling.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @Column(columnDefinition = "CHAR(36)", length = 36)
    private String id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false, columnDefinition = "CHAR(36)", length = 36)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditAction action;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    @Column(name = "old_value", columnDefinition = "json")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "json")
    private String newValue;

    @Column(name = "performed_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime performedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (performedAt == null) {
            performedAt = LocalDateTime.now();
        }
    }
}
