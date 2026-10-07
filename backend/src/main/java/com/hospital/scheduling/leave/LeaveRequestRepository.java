package com.hospital.scheduling.leave;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, String> {
    List<LeaveRequest> findByEmployeeId(String employeeId);
    List<LeaveRequest> findByStatus(LeaveStatus status);
    List<LeaveRequest> findByEmployeeIdAndStatus(String employeeId, LeaveStatus status);
    List<LeaveRequest> findByEmployeeDepartmentId(String departmentId);
    List<LeaveRequest> findByEmployeeDepartmentIdAndStatus(String departmentId, LeaveStatus status);

    @Query("SELECT lr FROM LeaveRequest lr WHERE " +
           "lr.employee.id = :employeeId AND " +
           "lr.status IN :statuses AND " +
           "(lr.startDate <= :endDate AND lr.endDate >= :startDate)")
    List<LeaveRequest> findOverlappingLeaves(
            @Param("employeeId") String employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Collection<LeaveStatus> statuses
    );
}
