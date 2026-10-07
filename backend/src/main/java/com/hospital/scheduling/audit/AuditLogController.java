package com.hospital.scheduling.audit;

import com.hospital.scheduling.audit.dto.AuditLogDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> result;

        if (entityType != null && !entityType.isBlank()) {
            result = auditLogRepository.findByEntityTypeOrderByPerformedAtDesc(entityType, pageable);
        } else {
            result = auditLogRepository.findAllByOrderByPerformedAtDesc(pageable);
        }

        Page<AuditLogDto> dtoPage = result.map(AuditLogDto::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }
}
