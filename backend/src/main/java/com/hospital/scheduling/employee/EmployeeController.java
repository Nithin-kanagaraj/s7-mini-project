package com.hospital.scheduling.employee;

import com.hospital.scheduling.common.security.UserPrincipal;
import com.hospital.scheduling.employee.dto.EmployeeRequestDto;
import com.hospital.scheduling.employee.dto.EmployeeResponseDto;
import com.hospital.scheduling.user.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<EmployeeResponseDto>> searchEmployees(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) EmployeeType employeeType,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @AuthenticationPrincipal UserPrincipal principal) {

        String effectiveDepartmentId = departmentId;

        // Enforce RBAC scoping for Dept Head and Worker
        if (principal.getRole() == Role.DEPT_HEAD) {
            if (departmentId != null && !departmentId.equals(principal.getDepartmentId())) {
                throw new AccessDeniedException("Department Heads can only view employees in their own department");
            }
            effectiveDepartmentId = principal.getDepartmentId();
        } else if (principal.getRole() == Role.WORKER) {
            // Worker is only allowed to view self profile via /api/employees/{id}
            throw new AccessDeniedException("Workers cannot list all employees");
        }

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(employeeService.searchEmployees(effectiveDepartmentId, employeeType, status, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeResponseDto> getEmployeeById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {

        EmployeeResponseDto emp = employeeService.getEmployeeById(id);

        // Enforce server-side RBAC scoping
        if (principal.getRole() == Role.WORKER && !id.equals(principal.getEmployeeId())) {
            throw new AccessDeniedException("Workers can only view their own employee profile");
        }
        if (principal.getRole() == Role.DEPT_HEAD && !emp.getDepartmentId().equals(principal.getDepartmentId())) {
            throw new AccessDeniedException("Department Heads can only view employees in their own department");
        }

        return ResponseEntity.ok(emp);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<EmployeeResponseDto> createEmployee(@Valid @RequestBody EmployeeRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createEmployee(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<EmployeeResponseDto> updateEmployee(
            @PathVariable String id,
            @Valid @RequestBody EmployeeRequestDto dto) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, dto));
    }

    @PatchMapping("/{id}/terminate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponseDto> terminateEmployee(@PathVariable String id) {
        return ResponseEntity.ok(employeeService.terminateEmployee(id));
    }
}
