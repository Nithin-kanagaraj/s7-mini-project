package com.hospital.scheduling.report.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FairnessReportDto {
    private String departmentId;
    private String departmentName;
    private Double totalShiftsVariance;
    private Double nightShiftsVariance;
    private Double weekendShiftsVariance;
    private List<EmployeeFairnessMetrics> employeeMetrics;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmployeeFairnessMetrics {
        private String employeeId;
        private String employeeName;
        private String employeeType;
        private Integer totalShiftCount;
        private Integer nightShiftCount;
        private Integer weekendShiftCount;
    }
}
