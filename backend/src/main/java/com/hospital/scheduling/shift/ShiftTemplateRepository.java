package com.hospital.scheduling.shift;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, String> {
    Optional<ShiftTemplate> findByName(String name);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, String id);

    @Query("SELECT MAX(st.durationHours) FROM ShiftTemplate st")
    Double findMaxDurationHours();
}
