package com.hospital.scheduling.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    Optional<Employee> findByContactEmail(String email);
    boolean existsByContactEmail(String email);
    boolean existsByContactEmailAndIdNot(String email, String id);
    long countByDepartmentId(String departmentId);

    List<Employee> findByDepartmentId(String departmentId);
    List<Employee> findByEmploymentStatus(EmploymentStatus status);
    List<Employee> findByDepartmentIdAndEmploymentStatus(String departmentId, EmploymentStatus status);

    @Query("SELECT e FROM Employee e WHERE " +
           "(:departmentId IS NULL OR e.department.id = :departmentId) AND " +
           "(:employeeType IS NULL OR e.employeeType = :employeeType) AND " +
           "(:status IS NULL OR e.employmentStatus = :status) AND " +
           "(:search IS NULL OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.contactEmail) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Employee> searchEmployees(
            @Param("departmentId") String departmentId,
            @Param("employeeType") EmployeeType employeeType,
            @Param("status") EmploymentStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
