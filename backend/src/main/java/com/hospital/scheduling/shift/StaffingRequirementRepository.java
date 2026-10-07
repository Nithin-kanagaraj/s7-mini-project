package com.hospital.scheduling.shift;

import com.hospital.scheduling.employee.EmployeeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffingRequirementRepository extends JpaRepository<StaffingRequirement, String> {
    List<StaffingRequirement> findByDepartmentIdAndShiftDate(String departmentId, LocalDate shiftDate);
    List<StaffingRequirement> findByShiftDateBetween(LocalDate startDate, LocalDate endDate);
    List<StaffingRequirement> findByDepartmentIdAndShiftDateBetween(String departmentId, LocalDate startDate, LocalDate endDate);

    Optional<StaffingRequirement> findByDepartmentIdAndShiftTemplateIdAndShiftDateAndEmployeeTypeAndRequiredSkillId(
            String departmentId, String shiftTemplateId, LocalDate shiftDate, EmployeeType employeeType, String requiredSkillId);
}
