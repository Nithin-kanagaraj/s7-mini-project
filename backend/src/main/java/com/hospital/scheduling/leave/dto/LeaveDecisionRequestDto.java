package com.hospital.scheduling.leave.dto;

import com.hospital.scheduling.leave.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveDecisionRequestDto {

    @NotNull(message = "Decision status is required (APPROVED or REJECTED)")
    private LeaveStatus status;

    private String note;
}
