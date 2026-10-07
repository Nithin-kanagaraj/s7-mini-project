package com.hospital.scheduling.notification.dto;

import com.hospital.scheduling.notification.Notification;
import com.hospital.scheduling.notification.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private String id;
    private String recipientUserId;
    private String recipientUsername;
    private NotificationType type;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationDto fromEntity(Notification entity) {
        return NotificationDto.builder()
                .id(entity.getId())
                .recipientUserId(entity.getRecipientUser() != null ? entity.getRecipientUser().getId() : null)
                .recipientUsername(entity.getRecipientUser() != null ? entity.getRecipientUser().getUsername() : null)
                .type(entity.getType())
                .message(entity.getMessage())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
