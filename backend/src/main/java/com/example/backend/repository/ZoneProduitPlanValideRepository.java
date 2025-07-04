package com.example.backend.repository;

import com.example.backend.model.ZoneProduitPlanValide;
import com.example.backend.model.PlanInventaire;
import com.example.backend.model.Zone;
import com.example.backend.model.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZoneProduitPlanValideRepository extends JpaRepository<ZoneProduitPlanValide, Long> {
    Optional<ZoneProduitPlanValide> findByPlanAndZoneAndProduit(PlanInventaire plan, Zone zone, Produit produit);
    List<ZoneProduitPlanValide> findByPlan(PlanInventaire plan);
    List<ZoneProduitPlanValide> findByPlanAndZone(PlanInventaire plan, Zone zone);
    List<ZoneProduitPlanValide> findByZoneAndProduit(Zone zone, Produit produit);
}
