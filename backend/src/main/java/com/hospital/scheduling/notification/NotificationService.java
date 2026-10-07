package com.hospital.scheduling.notification;

import com.hospital.scheduling.common.exception.ResourceNotFoundException;
import com.hospital.scheduling.common.security.UserPrincipal;
import com.hospital.scheduling.leave.LeaveRequest;
import com.hospital.scheduling.notification.dto.EmergencyNotificationRequestDto;
import com.hospital.scheduling.notification.dto.NotificationDto;
import com.hospital.scheduling.schedule.AssignmentStatus;
import com.hospital.scheduling.schedule.Schedule;
import com.hospital.scheduling.schedule.ScheduleAssignment;
import com.hospital.scheduling.schedule.ScheduleAssignmentRepository;
import com.hospital.scheduling.user.Role;
import com.hospital.scheduling.user.User;
import com.hospital.scheduling.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public NotificationDto createAndSend(User recipient, NotificationType type, String message) {
        if (recipient == null) {
            return null;
        }

        Notification notification = Notification.builder()
                .recipientUser(recipient)
                .type(type)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationDto dto = NotificationDto.fromEntity(saved);

        try {
            messagingTemplate.convertAndSendToUser(recipient.getUsername(), "/queue/notifications", dto);
        } catch (Exception e) {
            log.warn("Failed to deliver WebSocket notification to user {}: {}", recipient.getUsername(), e.getMessage());
        }

        return dto;
    }

    @Transactional
    public void notifySchedulePublished(Schedule schedule) {
        List<ScheduleAssignment> assignments = scheduleAssignmentRepository.findByScheduleId(schedule.getId());
        Set<User> recipients = new HashSet<>();

        for (ScheduleAssignment sa : assignments) {
            if (sa.getEmployee() != null) {
                userRepository.findByEmployeeId(sa.getEmployee().getId()).ifPresent(recipients::add);
            }
        }

        String msg = "A new schedule for " + schedule.getDepartment().getName() +
                " (" + schedule.getPeriodStart() + " to " + schedule.getPeriodEnd() + ") has been published.";

        for (User u : recipients) {
            createAndSend(u, NotificationType.SCHEDULE_PUBLISHED, msg);
        }
    }

    @Transactional
    public void notifyShiftChanged(ScheduleAssignment assignment) {
        if (assignment.getEmployee() == null) {
            return;
        }

        userRepository.findByEmployeeId(assignment.getEmployee().getId()).ifPresent(user -> {
            String msg = "Your shift on " + assignment.getAssignmentDate() +
                    " (" + (assignment.getShiftTemplate() != null ? assignment.getShiftTemplate().getName() : "Shift") +
                    ") has been updated.";
            createAndSend(user, NotificationType.SHIFT_CHANGED, msg);
        });
    }

    @Transactional
    public void notifyLeaveDecision(LeaveRequest leave) {
        if (leave.getEmployee() == null) {
            return;
        }

        userRepository.findByEmployeeId(leave.getEmployee().getId()).ifPresent(user -> {
            String msg = "Your leave request for " + leave.getStartDate() + " to " + leave.getEndDate() +
                    " has been " + leave.getStatus().name() + ".";
            createAndSend(user, NotificationType.LEAVE_DECISION, msg);
        });
    }

    @Transactional
    public void notifyLeaveConflict(LeaveRequest leave, List<ScheduleAssignment> affectedAssignments) {
        List<User> managers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN || u.getRole() == Role.SCHEDULER
                        || (u.getRole() == Role.DEPT_HEAD && u.getEmployee() != null
                        && u.getEmployee().getDepartment() != null
                        && leave.getEmployee() != null
                        && leave.getEmployee().getDepartment() != null
                        && u.getEmployee().getDepartment().getId().equals(leave.getEmployee().getDepartment().getId())))
                .collect(Collectors.toList());

        String msg = "CONFLICT ALERT: Leave approval for " + leave.getEmployee().getFirstName() + " " +
                leave.getEmployee().getLastName() + " created " + affectedAssignments.size() +
                " NEEDS_REASSIGNMENT shift(s) on a published schedule.";

        for (User mgr : managers) {
            createAndSend(mgr, NotificationType.CONFLICT, msg);
        }
    }

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public int sendShiftReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<ScheduleAssignment> upcoming = scheduleAssignmentRepository.findAll().stream()
                .filter(sa -> sa.getAssignmentDate() != null
                        && sa.getAssignmentDate().equals(tomorrow)
                        && sa.getStatus() == AssignmentStatus.ASSIGNED
                        && sa.getSchedule() != null
                        && "PUBLISHED".equals(sa.getSchedule().getStatus().name()))
                .collect(Collectors.toList());

        int sent = 0;
        for (ScheduleAssignment sa : upcoming) {
            if (sa.getEmployee() != null) {
                var maybeUser = userRepository.findByEmployeeId(sa.getEmployee().getId());
                if (maybeUser.isPresent()) {
                    String msg = "Reminder: You have an upcoming " +
                            (sa.getShiftTemplate() != null ? sa.getShiftTemplate().getName() : "") +
                            " shift scheduled tomorrow (" + tomorrow + ").";
                    createAndSend(maybeUser.get(), NotificationType.SHIFT_REMINDER, msg);
                    sent++;
                }
            }
        }
        return sent;
    }

    @Transactional
    public int sendEmergencyAlert(EmergencyNotificationRequestDto dto, UserPrincipal sender) {
        List<User> targetUsers;
        if (dto.getDepartmentId() != null && !dto.getDepartmentId().isBlank()) {
            targetUsers = userRepository.findAll().stream()
                    .filter(u -> u.getEmployee() != null
                            && u.getEmployee().getDepartment() != null
                            && dto.getDepartmentId().equals(u.getEmployee().getDepartment().getId()))
                    .collect(Collectors.toList());
        } else {
            targetUsers = userRepository.findAll();
        }

        String alertMsg = "EMERGENCY RESTAFFING ALERT: " + dto.getMessage();
        for (User u : targetUsers) {
            createAndSend(u, NotificationType.EMERGENCY, alertMsg);
        }
        log.info("Emergency restaffing alert sent by {} to {} recipient(s)", sender.getUsername(), targetUsers.size());
        return targetUsers.size();
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(UserPrincipal principal) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(principal.getId()).stream()
                .map(NotificationDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UserPrincipal principal) {
        return notificationRepository.countByRecipientUserIdAndIsReadFalse(principal.getId());
    }

    @Transactional
    public NotificationDto markAsRead(String id, UserPrincipal principal) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + id));

        if (!notification.getRecipientUser().getId().equals(principal.getId())) {
            throw new ResourceNotFoundException("Notification not found for current user");
        }

        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        return NotificationDto.fromEntity(saved);
    }
}
