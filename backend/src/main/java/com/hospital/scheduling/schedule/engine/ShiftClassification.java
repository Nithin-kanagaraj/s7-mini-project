package com.hospital.scheduling.schedule.engine;

import com.hospital.scheduling.shift.ShiftTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Shared night/weekend classification and fairness variance used by both the
 * CP-SAT objective and the fairness report so the two cannot drift apart.
 */
public final class ShiftClassification {

    private ShiftClassification() {
    }

    public static boolean isNight(ShiftTemplate shift) {
        if (shift == null) {
            return false;
        }
        String name = shift.getName() != null ? shift.getName().toLowerCase() : "";
        return name.contains("night")
                || (shift.getEndTime() != null
                && shift.getStartTime() != null
                && shift.getEndTime().isBefore(shift.getStartTime()));
    }

    public static boolean isWeekend(LocalDate date) {
        if (date == null) {
            return false;
        }
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /** Population variance (same metric the engine's fairness objective equalizes toward). */
    public static double populationVariance(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double sumSquares = 0.0;
        for (int value : values) {
            double diff = value - mean;
            sumSquares += diff * diff;
        }
        return Math.round((sumSquares / values.size()) * 10.0) / 10.0;
    }
}
