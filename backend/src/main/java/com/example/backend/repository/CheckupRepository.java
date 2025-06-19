package com.example.backend.repository;

import com.example.backend.model.Checkup;
import com.example.backend.model.CheckupType;
import com.example.backend.model.PlanInventaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckupRepository extends JpaRepository<Checkup, Long> {
    List<Checkup> findByPlanId(Long planId);
    List<Checkup> findByPlanIdAndType(Long planId, CheckupType type);

    List<Checkup> findByPlan(PlanInventaire plan);
}
