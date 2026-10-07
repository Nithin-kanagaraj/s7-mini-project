package com.hospital.scheduling.shift;

import com.hospital.scheduling.audit.AuditAction;
import com.hospital.scheduling.audit.AuditService;
import com.hospital.scheduling.common.exception.ResourceConflictException;
import com.hospital.scheduling.common.exception.ResourceNotFoundException;
import com.hospital.scheduling.department.Department;
import com.hospital.scheduling.department.DepartmentRepository;
import com.hospital.scheduling.shift.dto.*;
import com.hospital.scheduling.skill.EmployeeSkillRepository;
import com.hospital.scheduling.skill.Skill;
import com.hospital.scheduling.skill.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffingRequirementService {

    private final StaffingRequirementRepository staffingRequirementRepository;
    private final DepartmentRepository departmentRepository;
    private final ShiftTemplateRepository shiftTemplateRepository;
    private final SkillRepository skillRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<StaffingRequirementResponseDto> getRequirements(String departmentId, LocalDate startDate, LocalDate endDate) {
        List<StaffingRequirement> list;
        if (departmentId != null && startDate != null && endDate != null) {
            list = staffingRequirementRepository.findByDepartmentIdAndShiftDateBetween(departmentId, startDate, endDate);
        } else if (departmentId != null && startDate != null) {
            list = staffingRequirementRepository.findByDepartmentIdAndShiftDate(departmentId, startDate);
        } else if (startDate != null && endDate != null) {
            list = staffingRequirementRepository.findByShiftDateBetween(startDate, endDate);
        } else {
            list = staffingRequirementRepository.findAll();
        }

        return list.stream()
                .map(StaffingRequirementResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StaffingRequirementResponseDto getRequirementById(String id) {
        StaffingRequirement req = staffingRequirementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staffing requirement not found with ID: " + id));
        return StaffingRequirementResponseDto.fromEntity(req);
    }

    @Transactional
    public StaffingRequirementResponseDto createRequirement(StaffingRequirementRequestDto dto) {
        Department dept = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));

        ShiftTemplate shift = shiftTemplateRepository.findById(dto.getShiftTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Shift template not found with ID: " + dto.getShiftTemplateId()));

        Skill skill = null;
        if (dto.getRequiredSkillId() != null && !dto.getRequiredSkillId().isBlank()) {
            skill = skillRepository.findById(dto.getRequiredSkillId())
                    .orElseThrow(() -> new ResourceNotFoundException("Skill not found with ID: " + dto.getRequiredSkillId()));
        }

        Optional<StaffingRequirement> existing = staffingRequirementRepository
                .findByDepartmentIdAndShiftTemplateIdAndShiftDateAndEmployeeTypeAndRequiredSkillId(
                        dto.getDepartmentId(), dto.getShiftTemplateId(), dto.getShiftDate(), dto.getEmployeeType(), dto.getRequiredSkillId());

        if (existing.isPresent()) {
            throw new ResourceConflictException("Staffing requirement already exists for this department, date, shift, type and skill combination");
        }

        StaffingRequirement req = StaffingRequirement.builder()
                .department(dept)
                .shiftTemplate(shift)
                .shiftDate(dto.getShiftDate())
                .employeeType(dto.getEmployeeType())
                .requiredSkill(skill)
                .requiredCount(dto.getRequiredCount())
                .build();

        StaffingRequirement saved = staffingRequirementRepository.save(req);
        auditService.log("StaffingRequirement", saved.getId(), AuditAction.CREATE, null, saved);
        return StaffingRequirementResponseDto.fromEntity(saved);
    }

    @Transactional
    public StaffingRequirementResponseDto updateRequirement(String id, StaffingRequirementRequestDto dto) {
        StaffingRequirement req = staffingRequirementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staffing requirement not found with ID: " + id));

        Department dept = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));

        ShiftTemplate shift = shiftTemplateRepository.findById(dto.getShiftTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Shift template not found with ID: " + dto.getShiftTemplateId()));

        Skill skill = null;
        if (dto.getRequiredSkillId() != null && !dto.getRequiredSkillId().isBlank()) {
            skill = skillRepository.findById(dto.getRequiredSkillId())
                    .orElseThrow(() -> new ResourceNotFoundException("Skill not found with ID: " + dto.getRequiredSkillId()));
        }

        StaffingRequirement oldSnapshot = StaffingRequirement.builder()
                .id(req.getId())
                .department(req.getDepartment())
                .shiftTemplate(req.getShiftTemplate())
                .shiftDate(req.getShiftDate())
                .employeeType(req.getEmployeeType())
                .requiredSkill(req.getRequiredSkill())
                .requiredCount(req.getRequiredCount())
                .build();

        req.setDepartment(dept);
        req.setShiftTemplate(shift);
        req.setShiftDate(dto.getShiftDate());
        req.setEmployeeType(dto.getEmployeeType());
        req.setRequiredSkill(skill);
        req.setRequiredCount(dto.getRequiredCount());

        StaffingRequirement updated = staffingRequirementRepository.save(req);
        auditService.log("StaffingRequirement", updated.getId(), AuditAction.UPDATE, oldSnapshot, updated);
        return StaffingRequirementResponseDto.fromEntity(updated);
    }

    @Transactional
    public void deleteRequirement(String id) {
        StaffingRequirement req = staffingRequirementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staffing requirement not found with ID: " + id));

        staffingRequirementRepository.delete(req);
        auditService.log("StaffingRequirement", id, AuditAction.DELETE, req, null);
    }

    @Transactional(readOnly = true)
    public List<StaffingSkillWarningDto> getZeroQualifiedStaffWarnings(String departmentId, LocalDate startDate, LocalDate endDate) {
        List<StaffingRequirement> reqs;
        if (departmentId != null && startDate != null && endDate != null) {
            reqs = staffingRequirementRepository.findByDepartmentIdAndShiftDateBetween(departmentId, startDate, endDate);
        } else if (startDate != null && endDate != null) {
            reqs = staffingRequirementRepository.findByShiftDateBetween(startDate, endDate);
        } else {
            reqs = staffingRequirementRepository.findAll();
        }

        List<StaffingSkillWarningDto> warnings = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (StaffingRequirement req : reqs) {
            if (req.getRequiredSkill() != null && req.getRequiredCount() > 0) {
                long activeCount = employeeSkillRepository.countActiveCertifiedEmployeesInDepartment(
                        req.getDepartment().getId(), req.getRequiredSkill().getId(), today);

                if (activeCount == 0) {
                    warnings.add(StaffingSkillWarningDto.builder()
                            .requirementId(req.getId())
                            .departmentId(req.getDepartment().getId())
                            .departmentName(req.getDepartment().getName())
                            .shiftDate(req.getShiftDate())
                            .shiftTemplateName(req.getShiftTemplate() != null ? req.getShiftTemplate().getName() : "")
                            .employeeType(req.getEmployeeType())
                            .skillId(req.getRequiredSkill().getId())
                            .skillName(req.getRequiredSkill().getName())
                            .warningMessage("Zero active employees in department '" + req.getDepartment().getName() +
                                    "' currently hold the required skill '" + req.getRequiredSkill().getName() + "'")
                            .activeQualifiedEmployeeCount(0)
                            .build());
                }
            }
        }

        return warnings;
    }
}
