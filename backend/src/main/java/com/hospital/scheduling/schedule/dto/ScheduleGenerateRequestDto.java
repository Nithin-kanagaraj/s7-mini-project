package com.hospital.scheduling.schedule.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleGenerateRequestDto {

    @NotBlank(message = "Department ID is required")
    private String departmentId;

    @NotNull(message = "Period start date is required")
    @JsonAlias({"startDate"})
    private LocalDate periodStart;

    @NotNull(message = "Period end date is required")
    @JsonAlias({"endDate"})
    private LocalDate periodEnd;

    public LocalDate getStartDate() {
        return periodStart;
    }

    public void setStartDate(LocalDate startDate) {
        this.periodStart = startDate;
    }

    public LocalDate getEndDate() {
        return periodEnd;
    }

    public void setEndDate(LocalDate endDate) {
        this.periodEnd = endDate;
    }

    private Boolean archiveExisting;
}
