package com.hospital.scheduling.schedule.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortageItemReportDto {
    private String department;
    private String date;
    private String shift;
    private int required;
    private int assigned;
    private int shortage;
    private String reason; // INSUFFICIENT_QUALIFIED_STAFF, INSUFFICIENT_AVAILABLE_STAFF, MAX_HOURS_EXHAUSTED, REST_PERIOD_CONFLICT
    private String detail;
    private List<ShortageActionDto> possibleActions;
}
