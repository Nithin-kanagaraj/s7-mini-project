package com.hospital.scheduling.attendance;

import com.hospital.scheduling.attendance.dto.AttendanceClockInRequestDto;
import com.hospital.scheduling.attendance.dto.AttendanceRecordDto;
import com.hospital.scheduling.common.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','DEPT_HEAD')")
    public ResponseEntity<AttendanceRecordDto> recordAttendance(
            @PathVariable String assignmentId,
            @Valid @RequestBody AttendanceClockInRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.recordAttendance(assignmentId, dto, principal));
    }

    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AttendanceRecordDto>> getAttendanceByAssignment(@PathVariable String assignmentId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByAssignment(assignmentId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','DEPT_HEAD','SCHEDULER')")
    public ResponseEntity<List<AttendanceRecordDto>> getAllAttendance() {
        return ResponseEntity.ok(attendanceService.getAllAttendance());
    }
}
