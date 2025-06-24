package com.example.backend.repository;

import com.example.backend.model.PlanInventaire;
import com.example.backend.model.STATUS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface PlanInventaireRepository extends JpaRepository<PlanInventaire, Long> {
    long countByStatut(STATUS status);

    @Query("""
        SELECT z.name, 
        (1 - COUNT(DISTINCT e.id) * 1.0 / NULLIF(COUNT(DISTINCT p.id), 0)) * 100 as accuracy
        FROM Zone z 
        JOIN z.plans p
        JOIN p.produits prod
        LEFT JOIN Ecart e ON e.produit = prod AND e.planInventaire = p
        GROUP BY z.name
    """)
    List<Object[]> findZonePerformance();

    @Query("SELECT FUNCTION('MONTH', p.dateCreation) as month, COUNT(p) as count FROM PlanInventaire p WHERE p.statut = :statut GROUP BY FUNCTION('MONTH', p.dateCreation)")
    Map<String, Integer> countByStatutGroupByMonth(@Param("statut") STATUS statut);

    @Query("SELECT FUNCTION('MONTH', p.dateCreation) as month, COUNT(p) as count FROM PlanInventaire p GROUP BY FUNCTION('MONTH', p.dateCreation)")
    Map<String, Integer> countAllGroupByMonth();

    @Query("SELECT MAX(p.dateCreation) FROM PlanInventaire p JOIN p.zones z WHERE z.id = :zoneId")
    LocalDateTime findLastInventoryDateForZone(@Param("zoneId") Long zoneId);

    @Query("SELECT MAX(p.dateCreation) FROM PlanInventaire p JOIN p.produits prod WHERE prod.id = :productId")
    LocalDateTime findLastInventoryDateForProduct(@Param("productId") Long productId);

    // Fetch plans created by a given user id
    List<PlanInventaire> findByCreateurId(Long userId);
}
