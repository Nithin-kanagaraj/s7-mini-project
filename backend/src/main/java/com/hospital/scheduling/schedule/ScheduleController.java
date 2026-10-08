package com.hospital.scheduling.schedule;

import com.hospital.scheduling.common.security.UserPrincipal;
import com.hospital.scheduling.schedule.dto.*;
import com.hospital.scheduling.shift.StaffingRequirementService;
import com.hospital.scheduling.shift.dto.StaffingSkillWarningDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final StaffingRequirementService staffingRequirementService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ScheduleResponseDto>> getAllSchedules(
            @RequestParam(required = false) String departmentId) {
        return ResponseEntity.ok(scheduleService.getAllSchedules(departmentId));
    }

    @GetMapping("/my-assignments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ScheduleAssignmentResponseDto>> getMyAssignments(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(scheduleService.getMyAssignments(principal));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','SCHEDULER')")
    public ResponseEntity<ScheduleResponseDto> generateSchedule(
            @Valid @RequestBody ScheduleGenerateRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.generateSchedule(dto, principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ScheduleResponseDto> getScheduleById(@PathVariable String id) {
        return ResponseEntity.ok(scheduleService.getScheduleById(id));
    }

    @GetMapping("/{id}/pre-check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StaffingSkillWarningDto>> getPreCheckSanityWarnings(@PathVariable String id) {
        ScheduleResponseDto schedule = scheduleService.getScheduleById(id);
        return ResponseEntity.ok(staffingRequirementService.getZeroQualifiedStaffWarnings(
                schedule.getDepartmentId(), schedule.getPeriodStart(), schedule.getPeriodEnd()));
    }

    @PatchMapping("/{id}/assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','SCHEDULER')")
    public ResponseEntity<ScheduleAssignmentResponseDto> updateAssignment(
            @PathVariable String id,
            @PathVariable String assignmentId,
            @Valid @RequestBody AssignmentEditRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(scheduleService.updateAssignment(id, assignmentId, dto, principal));
    }

    @PatchMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','SCHEDULER')")
    public ResponseEntity<ScheduleAssignmentResponseDto> updateAssignmentLegacy(
            @PathVariable String assignmentId,
            @Valid @RequestBody AssignmentEditRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(scheduleService.updateAssignmentById(assignmentId, dto, principal));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','SCHEDULER')")
    public ResponseEntity<ScheduleResponseDto> publishSchedule(
            @PathVariable String id,
            @RequestBody(required = false) SchedulePublishRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(scheduleService.publishSchedule(id, dto, principal));
    }
}
