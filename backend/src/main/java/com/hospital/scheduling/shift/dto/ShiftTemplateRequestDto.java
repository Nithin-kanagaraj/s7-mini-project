package com.hospital.scheduling.shift.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftTemplateRequestDto {

    @NotBlank(message = "Shift name is required")
    @Size(max = 50, message = "Shift name must not exceed 50 characters")
    private String name;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Duration hours is required")
    @DecimalMin(value = "0.25", message = "Duration must be at least 0.25 hours")
    private BigDecimal durationHours;
}
