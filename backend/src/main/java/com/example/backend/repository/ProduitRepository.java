package com.example.backend.repository;

import com.example.backend.model.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {
    List<Produit> findByNomContainingOrDescriptionContainingOrCodeBarreContainingOrReferenceContaining(
            String nom, String description, String CodeBarre, String reference);

    List<Produit> findByCategoryId(Long categoryId);
    List<Produit> findBySubCategoryId(Long subCategoryId);

    Optional<Produit> findByCodeBarre(String codeBarre);

    @Query("SELECT DISTINCT p.unite FROM Produit p")
    List<String> findDistinctUnites();
}