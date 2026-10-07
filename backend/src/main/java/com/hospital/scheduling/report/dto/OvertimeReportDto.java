package com.hospital.scheduling.report.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimeReportDto {
    private String departmentId;
    private String departmentName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double totalOvertimeHours;
    private List<EmployeeOvertimeSummary> employeeSummaries;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmployeeOvertimeSummary {
        private String employeeId;
        private String employeeName;
        private String employeeType;
        private Double plannedHours;
        private Double actualHours;
        private Double overtimeHours;
        private Integer overtimeShiftCount;
    }
}
