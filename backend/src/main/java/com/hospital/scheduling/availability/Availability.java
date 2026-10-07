package com.hospital.scheduling.availability;

import com.hospital.scheduling.employee.Employee;
import com.hospital.scheduling.shift.ShiftTemplate;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "availability")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Availability {

    @Id
    @Column(columnDefinition = "CHAR(36)", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "unavailable_date", nullable = false)
    private LocalDate unavailableDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_template_id")
    private ShiftTemplate shiftTemplate;

    @Column(length = 255)
    private String reason;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
    }
}
