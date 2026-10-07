package com.hospital.scheduling.department;

import com.hospital.scheduling.audit.AuditAction;
import com.hospital.scheduling.audit.AuditService;
import com.hospital.scheduling.common.exception.ResourceConflictException;
import com.hospital.scheduling.common.exception.ResourceNotFoundException;
import com.hospital.scheduling.department.dto.DepartmentRequestDto;
import com.hospital.scheduling.department.dto.DepartmentResponseDto;
import com.hospital.scheduling.employee.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getAllDepartments(Boolean activeOnly) {
        List<Department> departments = Boolean.TRUE.equals(activeOnly)
                ? departmentRepository.findByIsActiveTrue()
                : departmentRepository.findAll();

        return departments.stream()
                .map(dept -> DepartmentResponseDto.fromEntity(dept, employeeRepository.countByDepartmentId(dept.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartmentResponseDto getDepartmentById(String id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
        return DepartmentResponseDto.fromEntity(dept, employeeRepository.countByDepartmentId(id));
    }

    @Transactional
    public DepartmentResponseDto createDepartment(DepartmentRequestDto dto) {
        if (departmentRepository.existsByName(dto.getName())) {
            throw new ResourceConflictException("Department with name '" + dto.getName() + "' already exists");
        }

        Department dept = Department.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        Department saved = departmentRepository.save(dept);
        auditService.log("Department", saved.getId(), AuditAction.CREATE, null, saved);

        return DepartmentResponseDto.fromEntity(saved, 0);
    }

    @Transactional
    public DepartmentResponseDto updateDepartment(String id, DepartmentRequestDto dto) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        if (departmentRepository.existsByNameAndIdNot(dto.getName(), id)) {
            throw new ResourceConflictException("Department with name '" + dto.getName() + "' already exists");
        }

        Department oldSnapshot = Department.builder()
                .id(dept.getId())
                .name(dept.getName())
                .description(dept.getDescription())
                .isActive(dept.getIsActive())
                .createdAt(dept.getCreatedAt())
                .build();

        dept.setName(dto.getName());
        dept.setDescription(dto.getDescription());
        if (dto.getIsActive() != null) {
            dept.setIsActive(dto.getIsActive());
        }

        Department updated = departmentRepository.save(dept);
        auditService.log("Department", updated.getId(), AuditAction.UPDATE, oldSnapshot, updated);

        return DepartmentResponseDto.fromEntity(updated, employeeRepository.countByDepartmentId(id));
    }

    @Transactional
    public void deleteOrDeactivateDepartment(String id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        long employeeCount = employeeRepository.countByDepartmentId(id);
        if (employeeCount > 0) {
            // Soft-deactivate if department has active/past employees
            Department oldSnapshot = Department.builder()
                    .id(dept.getId())
                    .name(dept.getName())
                    .description(dept.getDescription())
                    .isActive(dept.getIsActive())
                    .build();

            dept.setIsActive(false);
            Department updated = departmentRepository.save(dept);
            auditService.log("Department", updated.getId(), AuditAction.UPDATE, oldSnapshot, updated);
        } else {
            // Hard delete if no employees attached
            departmentRepository.delete(dept);
            auditService.log("Department", id, AuditAction.DELETE, dept, null);
        }
    }
}
