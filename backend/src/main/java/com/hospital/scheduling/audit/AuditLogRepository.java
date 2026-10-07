package com.hospital.scheduling.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    List<AuditLog> findByEntityTypeAndEntityIdOrderByPerformedAtDesc(String entityType, String entityId);
    Page<AuditLog> findAllByOrderByPerformedAtDesc(Pageable pageable);
    Page<AuditLog> findByEntityTypeOrderByPerformedAtDesc(String entityType, Pageable pageable);
}
