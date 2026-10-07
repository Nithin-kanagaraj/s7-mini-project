package com.hospital.scheduling.availability.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityRequestDto {

    @NotNull(message = "Unavailable date is required")
    private LocalDate unavailableDate;

    private String shiftTemplateId;

    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;
}
