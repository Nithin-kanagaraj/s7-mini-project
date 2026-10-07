package com.hospital.scheduling.compliance;

import com.hospital.scheduling.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "compliance_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceRule {

    @Id
    @Column(columnDefinition = "CHAR(36)", length = 36)
    private String id;

    @Column(name = "rule_key", nullable = false, unique = true, length = 50)
    private String ruleKey;

    @Column(name = "rule_value", nullable = false)
    private Integer ruleValue;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        updatedAt = LocalDateTime.now();
    }
}
