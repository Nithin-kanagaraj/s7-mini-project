package com.hospital.scheduling.notification;

import com.hospital.scheduling.common.security.UserPrincipal;
import com.hospital.scheduling.notification.dto.EmergencyNotificationRequestDto;
import com.hospital.scheduling.notification.dto.NotificationDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationDto>> getMyNotifications(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getUserNotifications(principal));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount(principal)));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationDto> markAsRead(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.markAsRead(id, principal));
    }

    @PostMapping("/emergency")
    @PreAuthorize("hasAnyRole('ADMIN','SCHEDULER','DEPT_HEAD')")
    public ResponseEntity<Map<String, Object>> sendEmergencyAlert(
            @Valid @RequestBody EmergencyNotificationRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {

        int recipientCount = notificationService.sendEmergencyAlert(dto, principal);
        return ResponseEntity.ok(Map.of(
                "message", "Emergency restaffing notification queued for in-app delivery",
                "recipientCount", recipientCount
        ));
    }

    @PostMapping("/shift-reminders")
    @PreAuthorize("hasAnyRole('ADMIN','SCHEDULER')")
    public ResponseEntity<Map<String, Object>> triggerShiftReminders() {
        int sent = notificationService.sendShiftReminders();
        return ResponseEntity.ok(Map.of(
                "message", "Shift reminder check completed",
                "sentCount", sent
        ));
    }
}
