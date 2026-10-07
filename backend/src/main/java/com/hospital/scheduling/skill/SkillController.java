package com.hospital.scheduling.skill;

import com.hospital.scheduling.skill.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping("/skills")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SkillResponseDto>> getAllSkills() {
        return ResponseEntity.ok(skillService.getAllSkills());
    }

    @GetMapping("/skills/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SkillResponseDto> getSkillById(@PathVariable String id) {
        return ResponseEntity.ok(skillService.getSkillById(id));
    }

    @PostMapping("/skills")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<SkillResponseDto> createSkill(@Valid @RequestBody SkillRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(skillService.createSkill(dto));
    }

    @PutMapping("/skills/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<SkillResponseDto> updateSkill(
            @PathVariable String id,
            @Valid @RequestBody SkillRequestDto dto) {
        return ResponseEntity.ok(skillService.updateSkill(id, dto));
    }

    @GetMapping("/employees/{employeeId}/skills")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EmployeeSkillResponseDto>> getEmployeeSkills(@PathVariable String employeeId) {
        return ResponseEntity.ok(skillService.getEmployeeSkills(employeeId));
    }

    @PostMapping("/employees/{employeeId}/skills")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<EmployeeSkillResponseDto> assignSkillToEmployee(
            @PathVariable String employeeId,
            @Valid @RequestBody EmployeeSkillAssignDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(skillService.assignSkillToEmployee(employeeId, dto));
    }

    @DeleteMapping("/employees/{employeeId}/skills/{skillId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Void> removeSkillFromEmployee(
            @PathVariable String employeeId,
            @PathVariable String skillId) {
        skillService.removeSkillFromEmployee(employeeId, skillId);
        return ResponseEntity.noContent().build();
    }
}
