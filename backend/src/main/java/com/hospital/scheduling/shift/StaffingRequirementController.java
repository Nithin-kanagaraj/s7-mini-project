package com.hospital.scheduling.shift;

import com.hospital.scheduling.common.security.UserPrincipal;
import com.hospital.scheduling.shift.dto.*;
import com.hospital.scheduling.user.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/staffing-requirements")
@RequiredArgsConstructor
public class StaffingRequirementController {

    private final StaffingRequirementService staffingRequirementService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StaffingRequirementResponseDto>> getRequirements(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal principal) {

        String effectiveDepartmentId = departmentId;
        if (principal.getRole() == Role.DEPT_HEAD) {
            if (departmentId != null && !departmentId.equals(principal.getDepartmentId())) {
                throw new AccessDeniedException("Department Heads can only view staffing requirements for their own department");
            }
            effectiveDepartmentId = principal.getDepartmentId();
        }

        return ResponseEntity.ok(staffingRequirementService.getRequirements(effectiveDepartmentId, startDate, endDate));
    }

    @GetMapping("/warnings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StaffingSkillWarningDto>> getZeroQualifiedStaffWarnings(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal principal) {

        String effectiveDepartmentId = departmentId;
        if (principal.getRole() == Role.DEPT_HEAD) {
            if (departmentId != null && !departmentId.equals(principal.getDepartmentId())) {
                throw new AccessDeniedException("Department Heads can only check warnings for their own department");
            }
            effectiveDepartmentId = principal.getDepartmentId();
        }

        return ResponseEntity.ok(staffingRequirementService.getZeroQualifiedStaffWarnings(effectiveDepartmentId, startDate, endDate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StaffingRequirementResponseDto> getRequirementById(@PathVariable String id) {
        return ResponseEntity.ok(staffingRequirementService.getRequirementById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SCHEDULER','DEPT_HEAD')")
    public ResponseEntity<StaffingRequirementResponseDto> createRequirement(
            @Valid @RequestBody StaffingRequirementRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal.getRole() == Role.DEPT_HEAD && !dto.getDepartmentId().equals(principal.getDepartmentId())) {
            throw new AccessDeniedException("Department Heads can only create staffing requirements for their own department");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(staffingRequirementService.createRequirement(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SCHEDULER','DEPT_HEAD')")
    public ResponseEntity<StaffingRequirementResponseDto> updateRequirement(
            @PathVariable String id,
            @Valid @RequestBody StaffingRequirementRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal.getRole() == Role.DEPT_HEAD && !dto.getDepartmentId().equals(principal.getDepartmentId())) {
            throw new AccessDeniedException("Department Heads can only update staffing requirements for their own department");
        }

        return ResponseEntity.ok(staffingRequirementService.updateRequirement(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SCHEDULER','DEPT_HEAD')")
    public ResponseEntity<Void> deleteRequirement(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal.getRole() == Role.DEPT_HEAD) {
            StaffingRequirementResponseDto existing = staffingRequirementService.getRequirementById(id);
            if (!existing.getDepartmentId().equals(principal.getDepartmentId())) {
                throw new AccessDeniedException("Department Heads can only delete staffing requirements for their own department");
            }
        }

        staffingRequirementService.deleteRequirement(id);
        return ResponseEntity.noContent().build();
    }
}
