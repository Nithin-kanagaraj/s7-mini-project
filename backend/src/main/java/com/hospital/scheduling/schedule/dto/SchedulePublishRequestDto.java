package com.hospital.scheduling.schedule.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulePublishRequestDto {
    private Boolean acknowledgeShortages;
}
