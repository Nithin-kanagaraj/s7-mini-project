package com.hospital.scheduling.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.scheduling.common.security.UserPrincipal;
import com.hospital.scheduling.user.User;
import com.hospital.scheduling.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void log(String entityType, String entityId, AuditAction action, Object oldValue, Object newValue) {
        User performedBy = getCurrentUser();
        if (performedBy == null) {
            log.warn("Attempted to record audit log without authenticated user for entity {} ID {}", entityType, entityId);
            return;
        }

        String oldJson = toJson(oldValue);
        String newJson = toJson(newValue);

        AuditLog logEntry = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .performedBy(performedBy)
                .oldValue(oldJson)
                .newValue(newJson)
                .performedAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(logEntry);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return userRepository.findById(principal.getId()).orElse(null);
        }
        return null;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize object to json for audit log", e);
            try {
                return objectMapper.writeValueAsString(String.valueOf(obj));
            } catch (Exception inner) {
                String safeText = String.valueOf(obj)
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"");
                return "\"" + safeText + "\"";
            }
        }
    }
}
