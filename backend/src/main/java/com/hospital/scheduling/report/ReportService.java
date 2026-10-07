package com.hospital.scheduling.report;

import com.hospital.scheduling.attendance.AttendanceRecord;
import com.hospital.scheduling.attendance.AttendanceRecordRepository;
import com.hospital.scheduling.department.Department;
import com.hospital.scheduling.department.DepartmentRepository;
import com.hospital.scheduling.employee.Employee;
import com.hospital.scheduling.employee.EmployeeRepository;
import com.hospital.scheduling.employee.EmploymentStatus;
import com.hospital.scheduling.report.dto.*;
import com.hospital.scheduling.schedule.AssignmentStatus;
import com.hospital.scheduling.schedule.ScheduleAssignment;
import com.hospital.scheduling.schedule.ScheduleAssignmentRepository;
import com.hospital.scheduling.schedule.engine.ShortageDiagnosticService;
import com.hospital.scheduling.shift.StaffingRequirement;
import com.hospital.scheduling.shift.StaffingRequirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final StaffingRequirementRepository staffingRequirementRepository;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ShortageDiagnosticService shortageDiagnosticService;

    @Transactional(readOnly = true)
    public CoverageReportDto getCoverageReport(String departmentId, LocalDate startDate, LocalDate endDate) {
        final LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusDays(14);
        final LocalDate end = (endDate != null) ? endDate : LocalDate.now().plusDays(14);

        Department dept = null;
        if (departmentId != null && !departmentId.isBlank()) {
            dept = departmentRepository.findById(departmentId).orElse(null);
        }

        List<StaffingRequirement> reqs = (dept != null)
                ? staffingRequirementRepository.findByDepartmentIdAndShiftDateBetween(dept.getId(), start, end)
                : staffingRequirementRepository.findAll().stream()
                .filter(r -> !r.getShiftDate().isBefore(start) && !r.getShiftDate().isAfter(end))
                .collect(Collectors.toList());

        int totalReq = reqs.stream().mapToInt(StaffingRequirement::getRequiredCount).sum();

        List<ScheduleAssignment> assignments = (dept != null)
                ? scheduleAssignmentRepository.findPublishedAssignmentsInDepartmentInRange(dept.getId(), start, end)
                : scheduleAssignmentRepository.findAll().stream()
                .filter(a -> !a.getAssignmentDate().isBefore(start) && !a.getAssignmentDate().isAfter(end))
                .collect(Collectors.toList());

        int totalAssigned = (int) assignments.stream().filter(a -> a.getStatus() == AssignmentStatus.ASSIGNED).count();
        int totalShortage = Math.max(0, totalReq - totalAssigned);

        double coveragePct = (totalReq > 0) ? (totalAssigned * 100.0 / totalReq) : 100.0;

        // Reason breakdown
        Map<String, Integer> reasonCounts = new HashMap<>();
        reasonCounts.put("INSUFFICIENT_QUALIFIED_STAFF", 0);
        reasonCounts.put("INSUFFICIENT_AVAILABLE_STAFF", 0);
        reasonCounts.put("MAX_HOURS_EXHAUSTED", 0);
        reasonCounts.put("REST_PERIOD_CONFLICT", 0);

        if (totalShortage > 0) {
            reasonCounts.put("INSUFFICIENT_QUALIFIED_STAFF", totalShortage);
        }

        // Daily breakdown
        Map<LocalDate, CoverageReportDto.DailyCoverageSummary> dailyMap = new TreeMap<>();
        LocalDate curr = startDate;
        while (!curr.isAfter(endDate)) {
            final LocalDate d = curr;
            int dReq = reqs.stream().filter(r -> r.getShiftDate().equals(d)).mapToInt(StaffingRequirement::getRequiredCount).sum();
            int dAssigned = (int) assignments.stream().filter(a -> a.getAssignmentDate().equals(d) && a.getStatus() == AssignmentStatus.ASSIGNED).count();
            int dShort = Math.max(0, dReq - dAssigned);
            double dPct = (dReq > 0) ? (dAssigned * 100.0 / dReq) : 100.0;

            dailyMap.put(d, CoverageReportDto.DailyCoverageSummary.builder()
                    .date(d)
                    .required(dReq)
                    .assigned(dAssigned)
                    .shortage(dShort)
                    .coveragePercent(Math.round(dPct * 10.0) / 10.0)
                    .build());

            curr = curr.plusDays(1);
        }

        return CoverageReportDto.builder()
                .departmentId(dept != null ? dept.getId() : null)
                .departmentName(dept != null ? dept.getName() : "All Departments")
                .startDate(startDate)
                .endDate(endDate)
                .totalRequiredSlots(totalReq)
                .totalAssignedSlots(totalAssigned)
                .totalShortageSlots(totalShortage)
                .coveragePercentage(Math.round(coveragePct * 10.0) / 10.0)
                .shortagesByReason(reasonCounts)
                .dailyBreakdown(new ArrayList<>(dailyMap.values()))
                .build();
    }

    @Transactional(readOnly = true)
    public OvertimeReportDto getOvertimeReport(String departmentId, LocalDate startDate, LocalDate endDate) {
        final LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusDays(14);
        final LocalDate end = (endDate != null) ? endDate : LocalDate.now().plusDays(14);

        Department dept = null;
        if (departmentId != null && !departmentId.isBlank()) {
            dept = departmentRepository.findById(departmentId).orElse(null);
        }

        List<Employee> emps = (dept != null)
                ? employeeRepository.findByDepartmentIdAndEmploymentStatus(dept.getId(), EmploymentStatus.ACTIVE)
                : employeeRepository.findAll().stream().filter(e -> e.getEmploymentStatus() == EmploymentStatus.ACTIVE).collect(Collectors.toList());

        List<OvertimeReportDto.EmployeeOvertimeSummary> summaries = new ArrayList<>();
        double totalOtHours = 0.0;

        for (Employee e : emps) {
            List<ScheduleAssignment> assignments = scheduleAssignmentRepository
                    .findByEmployeeIdAndAssignmentDateBetween(e.getId(), start, end);

            double planned = 0.0;
            double actual = 0.0;
            double otHours = 0.0;
            int otCount = 0;

            for (ScheduleAssignment sa : assignments) {
                if (sa.getStatus() != AssignmentStatus.ASSIGNED || sa.getShiftTemplate() == null) continue;

                double shiftPlanned = sa.getShiftTemplate().getDurationHours().doubleValue();
                planned += shiftPlanned;

                double shiftActual = shiftPlanned;
                List<AttendanceRecord> attendance = attendanceRecordRepository.findByAssignmentId(sa.getId());
                if (!attendance.isEmpty() && attendance.get(0).getClockIn() != null && attendance.get(0).getClockOut() != null) {
                    shiftActual = Duration.between(attendance.get(0).getClockIn(), attendance.get(0).getClockOut()).toMinutes() / 60.0;
                }
                actual += shiftActual;

                if (Boolean.TRUE.equals(sa.getIsOvertime())) {
                    otHours += shiftActual;
                    otCount++;
                }
            }

            totalOtHours += otHours;
            summaries.add(OvertimeReportDto.EmployeeOvertimeSummary.builder()
                    .employeeId(e.getId())
                    .employeeName(e.getFirstName() + " " + e.getLastName())
                    .employeeType(e.getEmployeeType().name())
                    .plannedHours(Math.round(planned * 10.0) / 10.0)
                    .actualHours(Math.round(actual * 10.0) / 10.0)
                    .overtimeHours(Math.round(otHours * 10.0) / 10.0)
                    .overtimeShiftCount(otCount)
                    .build());
        }

        return OvertimeReportDto.builder()
                .departmentId(dept != null ? dept.getId() : null)
                .departmentName(dept != null ? dept.getName() : "All Departments")
                .startDate(startDate)
                .endDate(endDate)
                .totalOvertimeHours(Math.round(totalOtHours * 10.0) / 10.0)
                .employeeSummaries(summaries)
                .build();
    }

    @Transactional(readOnly = true)
    public FairnessReportDto getFairnessReport(String departmentId) {
        Department dept = null;
        if (departmentId != null && !departmentId.isBlank()) {
            dept = departmentRepository.findById(departmentId).orElse(null);
        }

        List<Employee> emps = (dept != null)
                ? employeeRepository.findByDepartmentIdAndEmploymentStatus(dept.getId(), EmploymentStatus.ACTIVE)
                : employeeRepository.findAll().stream().filter(e -> e.getEmploymentStatus() == EmploymentStatus.ACTIVE).collect(Collectors.toList());

        List<FairnessReportDto.EmployeeFairnessMetrics> metrics = new ArrayList<>();
        List<Integer> totalCounts = new ArrayList<>();
        List<Integer> nightCounts = new ArrayList<>();
        List<Integer> weekendCounts = new ArrayList<>();

        for (Employee e : emps) {
            List<ScheduleAssignment> assignments = scheduleAssignmentRepository
                    .findByEmployeeIdAndAssignmentDateBetween(e.getId(), LocalDate.now().minusDays(30), LocalDate.now());

            int total = 0;
            int night = 0;
            int weekend = 0;

            for (ScheduleAssignment sa : assignments) {
                if (sa.getStatus() != AssignmentStatus.ASSIGNED || sa.getShiftTemplate() == null) continue;

                total++;
                boolean isNight = sa.getShiftTemplate().getName().toLowerCase().contains("night") ||
                        (sa.getShiftTemplate().getEndTime() != null && sa.getShiftTemplate().getEndTime().isBefore(sa.getShiftTemplate().getStartTime()));
                boolean isWeekend = (sa.getAssignmentDate().getDayOfWeek() == DayOfWeek.SATURDAY || sa.getAssignmentDate().getDayOfWeek() == DayOfWeek.SUNDAY);

                if (isNight) night++;
                if (isWeekend) weekend++;
            }

            totalCounts.add(total);
            nightCounts.add(night);
            weekendCounts.add(weekend);

            metrics.add(FairnessReportDto.EmployeeFairnessMetrics.builder()
                    .employeeId(e.getId())
                    .employeeName(e.getFirstName() + " " + e.getLastName())
                    .employeeType(e.getEmployeeType().name())
                    .totalShiftCount(total)
                    .nightShiftCount(night)
                    .weekendShiftCount(weekend)
                    .build());
        }

        return FairnessReportDto.builder()
                .departmentId(dept != null ? dept.getId() : null)
                .departmentName(dept != null ? dept.getName() : "All Departments")
                .totalShiftsVariance(computeVariance(totalCounts))
                .nightShiftsVariance(computeVariance(nightCounts))
                .weekendShiftsVariance(computeVariance(weekendCounts))
                .employeeMetrics(metrics)
                .build();
    }

    private double computeVariance(List<Integer> values) {
        if (values.isEmpty()) return 0.0;
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double temp = 0.0;
        for (int a : values) {
            temp += (a - mean) * (a - mean);
        }
        return Math.round((temp / values.size()) * 10.0) / 10.0;
    }
}
