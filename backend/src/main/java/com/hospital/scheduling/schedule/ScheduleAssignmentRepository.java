package com.hospital.scheduling.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleAssignmentRepository extends JpaRepository<ScheduleAssignment, String> {
    List<ScheduleAssignment> findByScheduleId(String scheduleId);
    void deleteByScheduleId(String scheduleId);

    List<ScheduleAssignment> findByEmployeeIdAndAssignmentDate(String employeeId, LocalDate assignmentDate);
    List<ScheduleAssignment> findByEmployee_IdAndAssignmentDate(String employeeId, LocalDate assignmentDate);
    List<ScheduleAssignment> findByEmployeeIdAndAssignmentDateBetween(String employeeId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT sa FROM ScheduleAssignment sa WHERE " +
           "sa.employee.id = :employeeId AND " +
           "(sa.schedule IS NULL OR sa.schedule.status = 'PUBLISHED') AND " +
           "sa.status IN ('ASSIGNED', 'PUBLISHED') AND " +
           "sa.assignmentDate BETWEEN :start AND :end")
    List<ScheduleAssignment> findPublishedAssignmentsForEmployeeInRange(
            @Param("employeeId") String employeeId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("SELECT sa FROM ScheduleAssignment sa WHERE " +
           "sa.department.id = :departmentId AND " +
           "(sa.schedule IS NULL OR sa.schedule.status = 'PUBLISHED') AND " +
           "sa.status IN ('ASSIGNED', 'PUBLISHED') AND " +
           "sa.assignmentDate BETWEEN :start AND :end")
    List<ScheduleAssignment> findPublishedAssignmentsInDepartmentInRange(
            @Param("departmentId") String departmentId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("SELECT sa FROM ScheduleAssignment sa WHERE " +
           "sa.employee.id = :employeeId AND " +
           "sa.schedule.status = 'PUBLISHED' AND " +
           "sa.status IN ('ASSIGNED', 'PUBLISHED') " +
           "ORDER BY sa.assignmentDate ASC")
    List<ScheduleAssignment> findMyPublishedAssignments(@Param("employeeId") String employeeId);
}
