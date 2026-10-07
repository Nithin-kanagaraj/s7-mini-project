package com.hospital.scheduling.availability;

import com.hospital.scheduling.audit.AuditAction;
import com.hospital.scheduling.audit.AuditService;
import com.hospital.scheduling.availability.dto.AvailabilityRequestDto;
import com.hospital.scheduling.availability.dto.AvailabilityResponseDto;
import com.hospital.scheduling.common.exception.InvalidOperationException;
import com.hospital.scheduling.common.exception.ResourceConflictException;
import com.hospital.scheduling.common.exception.ResourceNotFoundException;
import com.hospital.scheduling.employee.Employee;
import com.hospital.scheduling.employee.EmployeeRepository;
import com.hospital.scheduling.shift.ShiftTemplate;
import com.hospital.scheduling.shift.ShiftTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private static final int MAX_ADVANCE_MONTHS = 6;

    private final AvailabilityRepository availabilityRepository;
    private final EmployeeRepository employeeRepository;
    private final ShiftTemplateRepository shiftTemplateRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<AvailabilityResponseDto> getAvailabilityForEmployee(String employeeId, LocalDate startDate, LocalDate endDate) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
        }

        List<Availability> list;
        if (startDate != null && endDate != null) {
            list = availabilityRepository.findByEmployeeIdAndUnavailableDateBetween(employeeId, startDate, endDate);
        } else {
            list = availabilityRepository.findByEmployeeId(employeeId);
        }

        return list.stream()
                .map(AvailabilityResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public AvailabilityResponseDto createUnavailability(String employeeId, AvailabilityRequestDto dto) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        validateAdvanceLimit(dto.getUnavailableDate());

        ShiftTemplate shift = null;
        if (dto.getShiftTemplateId() != null && !dto.getShiftTemplateId().isBlank()) {
            shift = shiftTemplateRepository.findById(dto.getShiftTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shift template not found with ID: " + dto.getShiftTemplateId()));
        }

        Optional<Availability> existing = availabilityRepository
                .findByEmployeeIdAndUnavailableDateAndShiftTemplateId(employeeId, dto.getUnavailableDate(), dto.getShiftTemplateId());
        if (existing.isPresent()) {
            throw new ResourceConflictException("Unavailability entry already exists for this date and shift");
        }

        Availability avail = Availability.builder()
                .employee(emp)
                .unavailableDate(dto.getUnavailableDate())
                .shiftTemplate(shift)
                .reason(dto.getReason())
                .build();

        Availability saved = availabilityRepository.save(avail);
        auditService.log("Availability", saved.getId(), AuditAction.CREATE, null, saved);
        return AvailabilityResponseDto.fromEntity(saved);
    }

    @Transactional
    public AvailabilityResponseDto updateUnavailability(String id, String employeeId, AvailabilityRequestDto dto) {
        Availability avail = availabilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Availability entry not found with ID: " + id));

        if (!avail.getEmployee().getId().equals(employeeId)) {
            throw new InvalidOperationException("Cannot update another employee's availability");
        }

        validateAdvanceLimit(dto.getUnavailableDate());

        ShiftTemplate shift = null;
        if (dto.getShiftTemplateId() != null && !dto.getShiftTemplateId().isBlank()) {
            shift = shiftTemplateRepository.findById(dto.getShiftTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shift template not found with ID: " + dto.getShiftTemplateId()));
        }

        Availability oldSnapshot = Availability.builder()
                .id(avail.getId())
                .unavailableDate(avail.getUnavailableDate())
                .shiftTemplate(avail.getShiftTemplate())
                .reason(avail.getReason())
                .build();

        avail.setUnavailableDate(dto.getUnavailableDate());
        avail.setShiftTemplate(shift);
        avail.setReason(dto.getReason());

        Availability updated = availabilityRepository.save(avail);
        auditService.log("Availability", updated.getId(), AuditAction.UPDATE, oldSnapshot, updated);
        return AvailabilityResponseDto.fromEntity(updated);
    }

    @Transactional
    public void deleteUnavailability(String id, String employeeId) {
        Availability avail = availabilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Availability entry not found with ID: " + id));

        if (!avail.getEmployee().getId().equals(employeeId)) {
            throw new InvalidOperationException("Cannot delete another employee's availability");
        }

        availabilityRepository.delete(avail);
        auditService.log("Availability", id, AuditAction.DELETE, avail, null);
    }

    private void validateAdvanceLimit(LocalDate date) {
        LocalDate maxAllowedDate = LocalDate.now().plusMonths(MAX_ADVANCE_MONTHS);
        if (date != null && date.isAfter(maxAllowedDate)) {
            throw new InvalidOperationException("Availability cannot be entered more than 6 months in advance (maximum date allowed: " + maxAllowedDate + ")");
        }
    }
}
