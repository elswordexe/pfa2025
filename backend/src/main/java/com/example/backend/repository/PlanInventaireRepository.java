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

    List<PlanInventaire> findByCreateurId(Long userId);

    @Query("SELECT DISTINCT p FROM PlanInventaire p " +
           "LEFT JOIN FETCH p.zones " +
           "LEFT JOIN FETCH p.produits " +
           "LEFT JOIN FETCH p.createur " +
           "LEFT JOIN FETCH p.assignations a " +
           "LEFT JOIN FETCH a.agent " +
           "LEFT JOIN FETCH a.zone")
    List<PlanInventaire> findAllWithDetails();

    @Query("SELECT p FROM PlanInventaire p WHERE p.dateCreation BETWEEN :startDate AND :endDate")
    List<PlanInventaire> findByDateCreationBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, p.dateDebut, p.dateFin)) FROM PlanInventaire p WHERE p.statut = 'Termine'")
    Double getAverageCompletionTimeInHours();

    @Query("SELECT COUNT(p) FROM PlanInventaire p WHERE p.dateCreation >= :startDate")
    long countPlansCreatedSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT p.statut as statut, COUNT(p) as count FROM PlanInventaire p GROUP BY p.statut")
    List<Object[]> countByStatutGrouped();

    @Query("SELECT MONTH(p.dateCreation) as month, COUNT(p) as count FROM PlanInventaire p WHERE YEAR(p.dateCreation) = :year GROUP BY MONTH(p.dateCreation)")
    List<Object[]> countByMonth(@Param("year") int year);
}
