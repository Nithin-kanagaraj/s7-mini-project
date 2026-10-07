package com.hospital.scheduling.shift;

import com.hospital.scheduling.shift.dto.ShiftTemplateRequestDto;
import com.hospital.scheduling.shift.dto.ShiftTemplateResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shift-templates")
@RequiredArgsConstructor
public class ShiftTemplateController {

    private final ShiftTemplateService shiftTemplateService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ShiftTemplateResponseDto>> getAllShiftTemplates() {
        return ResponseEntity.ok(shiftTemplateService.getAllShiftTemplates());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ShiftTemplateResponseDto> getShiftTemplateById(@PathVariable String id) {
        return ResponseEntity.ok(shiftTemplateService.getShiftTemplateById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SCHEDULER')")
    public ResponseEntity<ShiftTemplateResponseDto> createShiftTemplate(@Valid @RequestBody ShiftTemplateRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shiftTemplateService.createShiftTemplate(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SCHEDULER')")
    public ResponseEntity<ShiftTemplateResponseDto> updateShiftTemplate(
            @PathVariable String id,
            @Valid @RequestBody ShiftTemplateRequestDto dto) {
        return ResponseEntity.ok(shiftTemplateService.updateShiftTemplate(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteShiftTemplate(@PathVariable String id) {
        shiftTemplateService.deleteShiftTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
