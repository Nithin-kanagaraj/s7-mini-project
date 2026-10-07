package com.hospital.scheduling.skill;

import com.hospital.scheduling.audit.AuditAction;
import com.hospital.scheduling.audit.AuditService;
import com.hospital.scheduling.common.exception.ResourceConflictException;
import com.hospital.scheduling.common.exception.ResourceNotFoundException;
import com.hospital.scheduling.employee.Employee;
import com.hospital.scheduling.employee.EmployeeRepository;
import com.hospital.scheduling.skill.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<SkillResponseDto> getAllSkills() {
        return skillRepository.findAll().stream()
                .map(SkillResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SkillResponseDto getSkillById(String id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with ID: " + id));
        return SkillResponseDto.fromEntity(skill);
    }

    @Transactional
    public SkillResponseDto createSkill(SkillRequestDto dto) {
        if (skillRepository.existsByName(dto.getName())) {
            throw new ResourceConflictException("Skill with name '" + dto.getName() + "' already exists");
        }

        Skill skill = Skill.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();

        Skill saved = skillRepository.save(skill);
        auditService.log("Skill", saved.getId(), AuditAction.CREATE, null, saved);
        return SkillResponseDto.fromEntity(saved);
    }

    @Transactional
    public SkillResponseDto updateSkill(String id, SkillRequestDto dto) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with ID: " + id));

        if (skillRepository.existsByNameAndIdNot(dto.getName(), id)) {
            throw new ResourceConflictException("Skill with name '" + dto.getName() + "' already exists");
        }

        Skill oldSnapshot = Skill.builder()
                .id(skill.getId())
                .name(skill.getName())
                .description(skill.getDescription())
                .build();

        skill.setName(dto.getName());
        skill.setDescription(dto.getDescription());

        Skill updated = skillRepository.save(skill);
        auditService.log("Skill", updated.getId(), AuditAction.UPDATE, oldSnapshot, updated);
        return SkillResponseDto.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public List<EmployeeSkillResponseDto> getEmployeeSkills(String employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
        }
        return employeeSkillRepository.findByEmployeeId(employeeId).stream()
                .map(EmployeeSkillResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public EmployeeSkillResponseDto assignSkillToEmployee(String employeeId, EmployeeSkillAssignDto dto) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        Skill skill = skillRepository.findById(dto.getSkillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with ID: " + dto.getSkillId()));

        EmployeeSkillId key = new EmployeeSkillId(employeeId, dto.getSkillId());
        EmployeeSkill empSkill = employeeSkillRepository.findById(key)
                .orElseGet(() -> EmployeeSkill.builder()
                        .id(key)
                        .employee(employee)
                        .skill(skill)
                        .build());

        empSkill.setCertifiedDate(dto.getCertifiedDate());
        empSkill.setExpiryDate(dto.getExpiryDate());

        EmployeeSkill saved = employeeSkillRepository.save(empSkill);
        auditService.log("EmployeeSkill", employeeId + ":" + dto.getSkillId(), AuditAction.UPDATE, null, saved);
        return EmployeeSkillResponseDto.fromEntity(saved);
    }

    @Transactional
    public void removeSkillFromEmployee(String employeeId, String skillId) {
        EmployeeSkillId key = new EmployeeSkillId(employeeId, skillId);
        EmployeeSkill empSkill = employeeSkillRepository.findById(key)
                .orElseThrow(() -> new ResourceNotFoundException("Employee skill assignment not found"));

        employeeSkillRepository.delete(empSkill);
        auditService.log("EmployeeSkill", employeeId + ":" + skillId, AuditAction.DELETE, empSkill, null);
    }
}
