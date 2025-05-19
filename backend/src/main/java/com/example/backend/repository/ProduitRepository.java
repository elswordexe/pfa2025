package com.example.backend.repository;

import com.example.backend.model.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProduitRepository extends JpaRepository<Produit, Long> {
    List<Produit> findByNomContainingOrDescriptionContainingOrCodeBarContainingOrReferenceContaining(
            String nom, String description, String codeBar, String reference);
    
    List<Produit> findByCategoryId(Long categoryId);
    Optional<Produit> findByCodeBar(String codeBar);

    @Query("SELECT DISTINCT p.unite FROM Produit p")
    List<String> findDistinctUnites();
}