package com.hospital.scheduling.schedule.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortageActionDto {
    private String action; // ASSIGN_OVERTIME, CROSS_DEPARTMENT_TRANSFER, REDUCE_REQUIREMENT, MANUAL_OVERRIDE
    private List<String> eligibleEmployees;
    private List<String> candidateDepartments;
    private String note;
}
