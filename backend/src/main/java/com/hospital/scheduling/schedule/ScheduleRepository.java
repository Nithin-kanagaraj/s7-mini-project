package com.hospital.scheduling.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, String> {
    List<Schedule> findByDepartmentId(String departmentId);
    List<Schedule> findByDepartmentIdAndStatus(String departmentId, ScheduleStatus status);

    @Query("SELECT s FROM Schedule s WHERE " +
           "s.department.id = :departmentId AND " +
           "(s.periodStart <= :end AND s.periodEnd >= :start)")
    List<Schedule> findOverlappingSchedules(
            @Param("departmentId") String departmentId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("SELECT s FROM Schedule s WHERE " +
           "s.department.id = :departmentId AND " +
           "s.status = 'PUBLISHED' AND " +
           "(s.periodStart <= :end AND s.periodEnd >= :start)")
    List<Schedule> findOverlappingPublishedSchedules(
            @Param("departmentId") String departmentId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
