package com.example.backend.repository;

import com.example.backend.model.Ecart;
import com.example.backend.model.EcartStatut;
import com.example.backend.model.EcartType;
import com.example.backend.model.PlanInventaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface EcartRepository extends JpaRepository<Ecart, Long> {
    List<Ecart> findByPlanInventaireId(Long planInventaireId);

    long countByPlanInventaire(PlanInventaire plan);

    @Query("SELECT c.name, COUNT(e) FROM Ecart e JOIN e.produit p JOIN p.category c GROUP BY c.name")
    List<Object[]> findDiscrepanciesByCategory();

    @Query(value = """
        SELECT DATE(e.date_creation) as date, COUNT(*) as count
        FROM ecarts e
        WHERE e.date_creation >= 
            CASE :timeframe
                WHEN 'week' THEN CURRENT_DATE - INTERVAL '7 days'
                WHEN 'month' THEN CURRENT_DATE - INTERVAL '30 days'
                WHEN 'quarter' THEN CURRENT_DATE - INTERVAL '90 days'
                ELSE CURRENT_DATE - INTERVAL '365 days'
            END
        GROUP BY DATE(e.date_creation)
        ORDER BY date
    """, nativeQuery = true)
    List<Object[]> findHistoricalDiscrepancies(@Param("timeframe") String timeframe);

    @Query("SELECT e FROM Ecart e WHERE ABS(e.ecartQuantite) > (e.produit.quantitetheo * 0.1)")
    List<Ecart> findSignificantEcarts();

    List<Ecart> findByPlanInventaireIdAndStatut(Long planId, EcartStatut statut);

    @Query("SELECT e FROM Ecart e WHERE e.statut = 'EN_ATTENTE' ORDER BY ABS(e.ecartQuantite) DESC")
    List<Ecart> findPendingEcartsOrderByMagnitude();

    @Query("SELECT COUNT(e) FROM Ecart e WHERE e.type = :type")
    long countByType(@Param("type") EcartType type);

    @Query("SELECT COUNT(e) FROM Ecart e WHERE e.statut = :statut")
    long countByStatut(@Param("statut") EcartStatut statut);

    @Query("SELECT e.type as type, COUNT(e) as count FROM Ecart e GROUP BY e.type")
    List<Object[]> countByTypeGrouped();

    @Query("SELECT e.statut as statut, COUNT(e) as count FROM Ecart e GROUP BY e.statut")
    List<Object[]> countByStatutGrouped();

    @Query("SELECT COUNT(e) FROM Ecart e WHERE e.planInventaire.id = :planId")
    long countByPlanId(@Param("planId") Long planId);

    @Query("SELECT e FROM Ecart e WHERE e.dateCreation BETWEEN :startDate AND :endDate")
    List<Ecart> findByDateCreationBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
