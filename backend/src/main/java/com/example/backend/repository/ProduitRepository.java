package com.example.backend.repository;

import com.example.backend.model.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProduitRepository extends JpaRepository<Produit, Long> {
    List<Produit> findByNomContainingOrDescriptionContainingOrCodeBarContainingOrReferenceContaining(
            String nom, String description, String codeBar, String reference);
    
    List<Produit> findByCategoryId(Long categoryId);
    
    @Query("SELECT DISTINCT p.unite FROM Produit p")
    List<String> findDistinctUnites();
}