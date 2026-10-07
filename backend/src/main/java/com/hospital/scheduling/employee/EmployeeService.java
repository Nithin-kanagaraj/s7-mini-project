package com.hospital.scheduling.employee;

import com.hospital.scheduling.audit.AuditAction;
import com.hospital.scheduling.audit.AuditService;
import com.hospital.scheduling.common.exception.InvalidOperationException;
import com.hospital.scheduling.common.exception.ResourceConflictException;
import com.hospital.scheduling.common.exception.ResourceNotFoundException;
import com.hospital.scheduling.department.Department;
import com.hospital.scheduling.department.DepartmentRepository;
import com.hospital.scheduling.employee.dto.EmployeeRequestDto;
import com.hospital.scheduling.employee.dto.EmployeeResponseDto;
import com.hospital.scheduling.shift.ShiftTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final ShiftTemplateRepository shiftTemplateRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<EmployeeResponseDto> searchEmployees(
            String departmentId,
            EmployeeType employeeType,
            EmploymentStatus status,
            String search,
            Pageable pageable) {

        return employeeRepository.searchEmployees(departmentId, employeeType, status, search, pageable)
                .map(EmployeeResponseDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public EmployeeResponseDto getEmployeeById(String id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
        return EmployeeResponseDto.fromEntity(emp);
    }

    @Transactional
    public EmployeeResponseDto createEmployee(EmployeeRequestDto dto) {
        if (employeeRepository.existsByContactEmail(dto.getContactEmail())) {
            throw new ResourceConflictException("Employee with email '" + dto.getContactEmail() + "' already exists");
        }

        Department dept = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));

        validateMaxWeeklyHours(dto.getMaxWeeklyHoursOverride());

        Employee emp = Employee.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .employeeType(dto.getEmployeeType())
                .department(dept)
                .contactEmail(dto.getContactEmail())
                .contactPhone(dto.getContactPhone())
                .employmentStatus(dto.getEmploymentStatus() != null ? dto.getEmploymentStatus() : EmploymentStatus.ACTIVE)
                .hireDate(dto.getHireDate())
                .maxWeeklyHoursOverride(dto.getMaxWeeklyHoursOverride())
                .build();

        Employee saved = employeeRepository.save(emp);
        auditService.log("Employee", saved.getId(), AuditAction.CREATE, null, saved);
        return EmployeeResponseDto.fromEntity(saved);
    }

    @Transactional
    public EmployeeResponseDto updateEmployee(String id, EmployeeRequestDto dto) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));

        if (employeeRepository.existsByContactEmailAndIdNot(dto.getContactEmail(), id)) {
            throw new ResourceConflictException("Employee with email '" + dto.getContactEmail() + "' already exists");
        }

        Department dept = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));

        validateMaxWeeklyHours(dto.getMaxWeeklyHoursOverride());

        Employee oldSnapshot = Employee.builder()
                .id(emp.getId())
                .firstName(emp.getFirstName())
                .lastName(emp.getLastName())
                .employeeType(emp.getEmployeeType())
                .department(emp.getDepartment())
                .contactEmail(emp.getContactEmail())
                .contactPhone(emp.getContactPhone())
                .employmentStatus(emp.getEmploymentStatus())
                .hireDate(emp.getHireDate())
                .maxWeeklyHoursOverride(emp.getMaxWeeklyHoursOverride())
                .build();

        emp.setFirstName(dto.getFirstName());
        emp.setLastName(dto.getLastName());
        emp.setEmployeeType(dto.getEmployeeType());
        emp.setDepartment(dept);
        emp.setContactEmail(dto.getContactEmail());
        emp.setContactPhone(dto.getContactPhone());
        if (dto.getEmploymentStatus() != null) {
            emp.setEmploymentStatus(dto.getEmploymentStatus());
        }
        emp.setHireDate(dto.getHireDate());
        emp.setMaxWeeklyHoursOverride(dto.getMaxWeeklyHoursOverride());

        Employee updated = employeeRepository.save(emp);
        auditService.log("Employee", updated.getId(), AuditAction.UPDATE, oldSnapshot, updated);
        return EmployeeResponseDto.fromEntity(updated);
    }

    @Transactional
    public EmployeeResponseDto terminateEmployee(String id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));

        if (emp.getEmploymentStatus() == EmploymentStatus.TERMINATED) {
            throw new InvalidOperationException("Employee is already terminated");
        }

        Employee oldSnapshot = Employee.builder()
                .id(emp.getId())
                .employmentStatus(emp.getEmploymentStatus())
                .build();

        emp.setEmploymentStatus(EmploymentStatus.TERMINATED);
        Employee updated = employeeRepository.save(emp);

        // TODO: In Phase 3/5, cascade employee termination to cancel/reassign future schedule_assignments and notify managers.

        auditService.log("Employee", updated.getId(), AuditAction.UPDATE, oldSnapshot, updated);
        return EmployeeResponseDto.fromEntity(updated);
    }

    private void validateMaxWeeklyHours(Integer maxWeeklyHours) {
        if (maxWeeklyHours == null) return;

        Double maxShiftDuration = shiftTemplateRepository.findMaxDurationHours();
        if (maxShiftDuration != null && maxWeeklyHours < maxShiftDuration) {
            throw new InvalidOperationException("Max weekly hours override (" + maxWeeklyHours +
                    " hrs) cannot be less than the longest shift template duration (" + maxShiftDuration + " hrs)");
        }
    }
}
