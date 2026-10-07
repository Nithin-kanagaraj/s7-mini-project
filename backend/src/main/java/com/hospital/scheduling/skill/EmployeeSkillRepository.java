package com.hospital.scheduling.skill;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmployeeSkillRepository extends JpaRepository<EmployeeSkill, EmployeeSkillId> {
    List<EmployeeSkill> findByEmployeeId(String employeeId);
    List<EmployeeSkill> findBySkillId(String skillId);

    @Query("SELECT COUNT(es) FROM EmployeeSkill es WHERE " +
           "es.skill.id = :skillId AND " +
           "es.employee.department.id = :departmentId AND " +
           "es.employee.employmentStatus = 'ACTIVE' AND " +
           "(es.expiryDate IS NULL OR es.expiryDate >= :currentDate)")
    long countActiveCertifiedEmployeesInDepartment(
            @Param("departmentId") String departmentId,
            @Param("skillId") String skillId,
            @Param("currentDate") LocalDate currentDate
    );
}
