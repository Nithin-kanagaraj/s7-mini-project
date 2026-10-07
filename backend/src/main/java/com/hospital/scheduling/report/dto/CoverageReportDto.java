package com.hospital.scheduling.report.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoverageReportDto {
    private String departmentId;
    private String departmentName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalRequiredSlots;
    private Integer totalAssignedSlots;
    private Integer totalShortageSlots;
    private Double coveragePercentage;
    private Map<String, Integer> shortagesByReason; // Reason enum -> count
    private List<DailyCoverageSummary> dailyBreakdown;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyCoverageSummary {
        private LocalDate date;
        private Integer required;
        private Integer assigned;
        private Integer shortage;
        private Double coveragePercent;
    }
}
