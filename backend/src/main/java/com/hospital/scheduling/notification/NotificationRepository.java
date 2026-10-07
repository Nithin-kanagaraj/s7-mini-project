package com.hospital.scheduling.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId);
    List<Notification> findByRecipientUserIdAndIsReadOrderByCreatedAtDesc(String recipientUserId, boolean isRead);
    long countByRecipientUserIdAndIsReadFalse(String recipientUserId);
}
