package com.example.backend.repository;

import com.example.backend.model.Zone;
import com.example.backend.model.ZoneProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ZoneRepository extends JpaRepository<Zone, Long> {
    @Query("SELECT zp FROM ZoneProduit zp")
    List<ZoneProduit> findAllZoneProduits();
    
    @Query("SELECT COUNT(zp) FROM ZoneProduit zp WHERE zp.produit.id = :productId AND zp.quantiteReelle != zp.quantiteTheorique")
    int countEcartsForProduct(@Param("productId") Long productId);
    
    @Query("SELECT SUM(zp.quantiteReelle) FROM ZoneProduit zp WHERE zp.produit.id = :productId")
    int sumRealQuantityForProduct(@Param("productId") Long productId);
}
