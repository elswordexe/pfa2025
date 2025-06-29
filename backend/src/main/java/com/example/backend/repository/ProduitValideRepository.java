package com.example.backend.repository;

import com.example.backend.model.ProduitValide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProduitValideRepository extends JpaRepository<ProduitValide, Long> {
    List<ProduitValide> findByPlanId(Long planId);
    void deleteByPlanId(Long planId);
    boolean existsByProduitIdAndZoneIdAndPlanId(Long produitId, Long zoneId, Long planId);
} 