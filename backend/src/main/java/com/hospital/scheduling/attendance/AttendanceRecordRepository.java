package com.hospital.scheduling.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, String> {
    List<AttendanceRecord> findByAssignmentId(String assignmentId);
}
