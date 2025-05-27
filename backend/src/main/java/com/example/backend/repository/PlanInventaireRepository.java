package com.example.backend.repository;

import com.example.backend.model.PlanInventaire;
import com.example.backend.model.STATUS;
import org.springframework.data.jpa.repository.JpaRepository;


public interface PlanInventaireRepository extends JpaRepository<PlanInventaire, Long> {
    long countByStatus(STATUS role);
}
