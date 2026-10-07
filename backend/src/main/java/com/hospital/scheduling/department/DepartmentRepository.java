package com.hospital.scheduling.department;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {
    Optional<Department> findByName(String name);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, String id);
    List<Department> findByIsActiveTrue();
}
