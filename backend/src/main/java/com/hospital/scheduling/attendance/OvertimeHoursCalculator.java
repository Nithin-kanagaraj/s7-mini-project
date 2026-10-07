package com.hospital.scheduling.attendance;

import com.hospital.scheduling.compliance.ComplianceRuleRepository;
import com.hospital.scheduling.employee.Employee;
import com.hospital.scheduling.schedule.AssignmentStatus;
import com.hospital.scheduling.schedule.ScheduleAssignment;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OvertimeHoursCalculator {

    private final ComplianceRuleRepository complianceRuleRepository;

    @Getter
    @Builder
    public static class AssignmentHours {
        private final double plannedHours;
        private final double countableHours;
        private final boolean hasActual;
    }

    public double maxWeeklyHours(Employee employee) {
        if (employee != null && employee.getMaxWeeklyHoursOverride() != null) {
            return employee.getMaxWeeklyHoursOverride().doubleValue();
        }
        return complianceRuleRepository.findByRuleKey("MAX_WEEKLY_HOURS")
                .map(r -> r.getRuleValue().doubleValue())
                .orElse(40.0);
    }

    public AssignmentHours hoursFor(ScheduleAssignment assignment, List<AttendanceRecord> attendance) {
        double planned = 0.0;
        if (assignment != null && assignment.getShiftTemplate() != null
                && assignment.getShiftTemplate().getDurationHours() != null) {
            planned = assignment.getShiftTemplate().getDurationHours().doubleValue();
        }

        if (attendance != null && !attendance.isEmpty()) {
            AttendanceRecord record = attendance.get(0);
            if (record.getClockIn() != null && record.getClockOut() != null) {
                LocalDateTime out = record.getClockOut();
                if (out.isBefore(record.getClockIn())) {
                    out = out.plusDays(1);
                }
                double actual = Duration.between(record.getClockIn(), out).toMinutes() / 60.0;
                return AssignmentHours.builder()
                        .plannedHours(planned)
                        .countableHours(actual)
                        .hasActual(true)
                        .build();
            }
        }

        return AssignmentHours.builder()
                .plannedHours(planned)
                .countableHours(planned)
                .hasActual(false)
                .build();
    }

    public boolean isCountableAssignment(ScheduleAssignment assignment) {
        return assignment != null
                && assignment.getStatus() == AssignmentStatus.ASSIGNED
                && assignment.getShiftTemplate() != null;
    }

    public double overtimeBeyondCap(double weeklyCountableHours, double maxWeekly) {
        return Math.max(0.0, weeklyCountableHours - maxWeekly);
    }
}
