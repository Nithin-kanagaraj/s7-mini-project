package com.hospital.scheduling.availability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, String> {
    List<Availability> findByEmployeeId(String employeeId);
    List<Availability> findByEmployeeIdAndUnavailableDate(String employeeId, LocalDate unavailableDate);
    List<Availability> findByEmployeeIdAndUnavailableDateBetween(String employeeId, LocalDate startDate, LocalDate endDate);
    Optional<Availability> findByEmployeeIdAndUnavailableDateAndShiftTemplateId(String employeeId, LocalDate unavailableDate, String shiftTemplateId);
}
