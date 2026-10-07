package com.hospital.scheduling.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceClockInRequestDto {
    @NotNull(message = "Clock-in timestamp is required")
    private LocalDateTime clockIn;

    @NotNull(message = "Clock-out timestamp is required")
    private LocalDateTime clockOut;

    /**
     * Explicit overtime approval. Excess hours are recorded either way, but
     * {@code schedule_assignments.is_overtime} is only set when this is true
     * (or the assignment was already overtime-approved via manual edit).
     */
    private Boolean approveOvertime;
}
