package com.hospital.scheduling.attendance;

import com.hospital.scheduling.schedule.ScheduleAssignment;
import com.hospital.scheduling.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord {

    @Id
    @Column(columnDefinition = "CHAR(36)", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private ScheduleAssignment assignment;

    @Column(name = "clock_in")
    private LocalDateTime clockIn;

    @Column(name = "clock_out")
    private LocalDateTime clockOut;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_method", nullable = false, columnDefinition = "VARCHAR(20)", length = 20)
    @Builder.Default
    private EntryMethod entryMethod = EntryMethod.MANUAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
    }
}
