package com.hospital.scheduling.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyNotificationRequestDto {
    private String departmentId;

    @NotBlank(message = "Emergency message content is required")
    private String message;
}
