package com.hospital.scheduling.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.scheduling.common.security.UserPrincipal;
import com.hospital.scheduling.user.Role;
import com.hospital.scheduling.user.User;
import com.hospital.scheduling.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void log_encodesStringValuesAsJson() throws Exception {
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        AuditService auditService = new AuditService(auditLogRepository, userRepository, objectMapper);

        User user = User.builder()
                .id("user-1")
                .username("admin")
                .passwordHash("hashed")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        UserPrincipal principal = new UserPrincipal(
                "user-1",
                "admin",
                "hashed",
                Role.ADMIN,
                null,
                null,
                null,
                null,
                null,
                true,
                Collections.emptyList()
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        auditService.log("Schedule", "schedule-1", AuditAction.UPDATE, "old value", "Invalid value.");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getOldValue()).isEqualTo("\"old value\"");
        assertThat(saved.getNewValue()).isEqualTo("\"Invalid value.\"");
        assertThat(objectMapper.readTree(saved.getNewValue()).asText()).isEqualTo("Invalid value.");
    }
}
