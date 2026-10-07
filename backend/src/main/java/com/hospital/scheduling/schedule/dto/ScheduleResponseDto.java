package com.hospital.scheduling.schedule.dto;

import com.hospital.scheduling.schedule.Schedule;
import com.hospital.scheduling.schedule.ScheduleStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponseDto {
    private String id;
    private String departmentId;
    private String departmentName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private ScheduleStatus status;
    private String generatedById;
    private String generatedByName;
    private LocalDateTime generatedAt;
    private LocalDateTime publishedAt;
    private Boolean hasShortages;
    private String solveStatus; // OPTIMAL, FEASIBLE, TIMEOUT_PARTIAL, INFEASIBLE
    private Long solveTimeMs;
    private List<ScheduleAssignmentResponseDto> assignments;
    private List<ShortageItemReportDto> shortageReport;

    public static ScheduleResponseDto fromEntity(Schedule s, String solveStatus, Long solveTimeMs, List<ScheduleAssignmentResponseDto> assignments, List<ShortageItemReportDto> shortageReport) {
        return ScheduleResponseDto.builder()
                .id(s.getId())
                .departmentId(s.getDepartment() != null ? s.getDepartment().getId() : null)
                .departmentName(s.getDepartment() != null ? s.getDepartment().getName() : null)
                .periodStart(s.getPeriodStart())
                .periodEnd(s.getPeriodEnd())
                .status(s.getStatus())
                .generatedById(s.getGeneratedBy() != null ? s.getGeneratedBy().getId() : null)
                .generatedByName(s.getGeneratedBy() != null ? s.getGeneratedBy().getUsername() : null)
                .generatedAt(s.getGeneratedAt())
                .publishedAt(s.getPublishedAt())
                .hasShortages(s.getHasShortages())
                .solveStatus(solveStatus)
                .solveTimeMs(solveTimeMs)
                .assignments(assignments)
                .shortageReport(shortageReport)
                .build();
    }
}
