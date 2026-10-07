package com.hospital.scheduling.skill;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, String> {
    Optional<Skill> findByName(String name);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, String id);
}
