package com.hospital.scheduling.department;

import com.hospital.scheduling.department.dto.DepartmentRequestDto;
import com.hospital.scheduling.department.dto.DepartmentResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DepartmentResponseDto>> getAllDepartments(
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        return ResponseEntity.ok(departmentService.getAllDepartments(activeOnly));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DepartmentResponseDto> getDepartmentById(@PathVariable String id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponseDto> createDepartment(@Valid @RequestBody DepartmentRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.createDepartment(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponseDto> updateDepartment(
            @PathVariable String id,
            @Valid @RequestBody DepartmentRequestDto dto) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDepartment(@PathVariable String id) {
        departmentService.deleteOrDeactivateDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
