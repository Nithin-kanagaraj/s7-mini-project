package com.hospital.scheduling.availability;

import com.hospital.scheduling.availability.dto.AvailabilityRequestDto;
import com.hospital.scheduling.availability.dto.AvailabilityResponseDto;
import com.hospital.scheduling.common.security.UserPrincipal;
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
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AvailabilityResponseDto>> getAvailabilityForEmployee(
            @PathVariable String employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal.getRole() == Role.WORKER && !employeeId.equals(principal.getEmployeeId())) {
            throw new AccessDeniedException("Workers can only view their own availability");
        }

        return ResponseEntity.ok(availabilityService.getAvailabilityForEmployee(employeeId, startDate, endDate));
    }

    @PostMapping("/my-availability")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AvailabilityResponseDto> createMyUnavailability(
            @Valid @RequestBody AvailabilityRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal.getEmployeeId() == null) {
            throw new AccessDeniedException("Authenticated user has no associated employee profile");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(availabilityService.createUnavailability(principal.getEmployeeId(), dto));
    }

    @PutMapping("/my-availability/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AvailabilityResponseDto> updateMyUnavailability(
            @PathVariable String id,
            @Valid @RequestBody AvailabilityRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal.getEmployeeId() == null) {
            throw new AccessDeniedException("Authenticated user has no associated employee profile");
        }

        return ResponseEntity.ok(availabilityService.updateUnavailability(id, principal.getEmployeeId(), dto));
    }

    @DeleteMapping("/my-availability/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMyUnavailability(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal.getEmployeeId() == null) {
            throw new AccessDeniedException("Authenticated user has no associated employee profile");
        }

        availabilityService.deleteUnavailability(id, principal.getEmployeeId());
        return ResponseEntity.noContent().build();
    }
}
