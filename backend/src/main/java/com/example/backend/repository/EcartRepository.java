package com.example.backend.repository;

import com.example.backend.model.Ecart;
import com.example.backend.model.EcartStatut;
import com.example.backend.model.EcartType;
import com.example.backend.model.PlanInventaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    Long countByType(EcartType type);

    Long countByStatut(EcartStatut statut);

}
