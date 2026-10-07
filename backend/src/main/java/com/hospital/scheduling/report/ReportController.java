package com.hospital.scheduling.report;

import com.hospital.scheduling.report.dto.CoverageReportDto;
import com.hospital.scheduling.report.dto.FairnessReportDto;
import com.hospital.scheduling.report.dto.OvertimeReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/coverage")
    @PreAuthorize("hasAnyRole('ADMIN','HR','SCHEDULER','DEPT_HEAD')")
    public ResponseEntity<CoverageReportDto> getCoverageReport(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(reportService.getCoverageReport(departmentId, startDate, endDate));
    }

    @GetMapping("/overtime")
    @PreAuthorize("hasAnyRole('ADMIN','HR','SCHEDULER','DEPT_HEAD')")
    public ResponseEntity<OvertimeReportDto> getOvertimeReport(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(reportService.getOvertimeReport(departmentId, startDate, endDate));
    }

    @GetMapping("/fairness")
    @PreAuthorize("hasAnyRole('ADMIN','HR','SCHEDULER','DEPT_HEAD')")
    public ResponseEntity<FairnessReportDto> getFairnessReport(
            @RequestParam(required = false) String departmentId) {

        return ResponseEntity.ok(reportService.getFairnessReport(departmentId));
    }
}
