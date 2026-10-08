package com.hospital.scheduling.leave;

import com.hospital.scheduling.employee.Employee;
import com.hospital.scheduling.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class LeaveRequest {

    public LeaveRequest() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    @Id
    @Column(columnDefinition = "CHAR(36)", length = 36)
    private String id;

    public void setId(String id) {
        this.id = (id != null) ? id : java.util.UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 20)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LeaveStatus status = LeaveStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public void setLeaveType(String leaveType) {
        if (leaveType == null || leaveType.isBlank()) {
            this.leaveType = LeaveType.ANNUAL;
            return;
        }
        this.leaveType = LeaveType.valueOf(leaveType.trim().toUpperCase());
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }

    public void setStatus(String status) {
        if (status == null || status.isBlank()) {
            this.status = LeaveStatus.PENDING;
            return;
        }
        this.status = LeaveStatus.valueOf(status.trim().toUpperCase());
    }

    public LocalDateTime getCreatedAt() {
        return this.requestedAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.requestedAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
    }
}
