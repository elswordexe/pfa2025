package com.example.backend.repository;

import com.example.backend.model.Checkup;
import com.example.backend.model.PlanInventaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CheckupRepository extends JpaRepository<Checkup, Long> {
    @Query("SELECT c FROM Checkup c WHERE c.plan.id = :planId")
    List<Checkup> findByPlanId(@Param("planId") Long planId);

    List<Checkup> findByPlan(PlanInventaire plan);

    @Query("SELECT c FROM Checkup c WHERE c.dateCheck BETWEEN :startDate AND :endDate")
    List<Checkup> findByDateCheckBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(c) FROM Checkup c WHERE c.demandeRecomptage = true AND c.plan.id = :planId")
    long countRecomptagesByPlanId(@Param("planId") Long planId);

    @Query("SELECT COUNT(c) FROM Checkup c WHERE c.valide = true AND c.plan.id = :planId")
    long countValidatedByPlanId(@Param("planId") Long planId);
}
