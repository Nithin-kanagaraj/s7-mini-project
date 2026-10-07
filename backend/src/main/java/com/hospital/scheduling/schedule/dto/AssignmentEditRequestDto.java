package com.hospital.scheduling.schedule.dto;

import com.hospital.scheduling.schedule.AssignmentStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentEditRequestDto {
    private String employeeId;
    private AssignmentStatus status;
    private Boolean isOvertime;
    private Integer version;
}
