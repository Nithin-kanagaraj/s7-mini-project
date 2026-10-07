package com.hospital.scheduling.shift;

import com.hospital.scheduling.audit.AuditAction;
import com.hospital.scheduling.audit.AuditService;
import com.hospital.scheduling.common.exception.ResourceConflictException;
import com.hospital.scheduling.common.exception.ResourceNotFoundException;
import com.hospital.scheduling.shift.dto.ShiftTemplateRequestDto;
import com.hospital.scheduling.shift.dto.ShiftTemplateResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftTemplateService {

    private final ShiftTemplateRepository shiftTemplateRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ShiftTemplateResponseDto> getAllShiftTemplates() {
        return shiftTemplateRepository.findAll().stream()
                .map(ShiftTemplateResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ShiftTemplateResponseDto getShiftTemplateById(String id) {
        ShiftTemplate st = shiftTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift template not found with ID: " + id));
        return ShiftTemplateResponseDto.fromEntity(st);
    }

    @Transactional
    public ShiftTemplateResponseDto createShiftTemplate(ShiftTemplateRequestDto dto) {
        if (shiftTemplateRepository.existsByName(dto.getName())) {
            throw new ResourceConflictException("Shift template with name '" + dto.getName() + "' already exists");
        }

        ShiftTemplate st = ShiftTemplate.builder()
                .name(dto.getName())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .durationHours(dto.getDurationHours())
                .build();

        ShiftTemplate saved = shiftTemplateRepository.save(st);
        auditService.log("ShiftTemplate", saved.getId(), AuditAction.CREATE, null, saved);
        return ShiftTemplateResponseDto.fromEntity(saved);
    }

    @Transactional
    public ShiftTemplateResponseDto updateShiftTemplate(String id, ShiftTemplateRequestDto dto) {
        ShiftTemplate st = shiftTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift template not found with ID: " + id));

        if (shiftTemplateRepository.existsByNameAndIdNot(dto.getName(), id)) {
            throw new ResourceConflictException("Shift template with name '" + dto.getName() + "' already exists");
        }

        ShiftTemplate oldSnapshot = ShiftTemplate.builder()
                .id(st.getId())
                .name(st.getName())
                .startTime(st.getStartTime())
                .endTime(st.getEndTime())
                .durationHours(st.getDurationHours())
                .build();

        st.setName(dto.getName());
        st.setStartTime(dto.getStartTime());
        st.setEndTime(dto.getEndTime());
        st.setDurationHours(dto.getDurationHours());

        ShiftTemplate updated = shiftTemplateRepository.save(st);
        auditService.log("ShiftTemplate", updated.getId(), AuditAction.UPDATE, oldSnapshot, updated);
        return ShiftTemplateResponseDto.fromEntity(updated);
    }

    @Transactional
    public void deleteShiftTemplate(String id) {
        ShiftTemplate st = shiftTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift template not found with ID: " + id));

        shiftTemplateRepository.delete(st);
        auditService.log("ShiftTemplate", id, AuditAction.DELETE, st, null);
    }
}
