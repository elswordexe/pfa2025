package com.example.backend.controller;

import com.example.backend.model.Produit;
import com.example.backend.repository.ProduitRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@Tag(name = "controller des Produits", description = "APIs des Produits")
public class ProduitController {
    @Autowired
    ProduitRepository produitRepository;
    
    @Operation(summary = "Lister tous les Produit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "lister les produits avec succès"),
    })
    @GetMapping("Produits")
    public Iterable<Produit> getAllProduits(){
        return produitRepository.findAll();
    }

    @Operation(summary = "Retourner les infos d'un seul produit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produit listé avec succès"),
            @ApiResponse(responseCode = "404", description = "Produit non trouvé")
    })
    @GetMapping("Produits/{produitId}")
    public Produit getProduitById(@PathVariable Long produitId){
        return produitRepository.findById(produitId).orElse(null);
    }

    @Operation(summary = "Ajout d'un nouveau produit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produit ajouté avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    @PostMapping("Produits/register")
    public ResponseEntity<?> registerProduit(@RequestBody Produit produit){
        Produit produit1 = produitRepository.save(produit);
        return ResponseEntity.ok(produit1);
    }

    @Operation(summary = "Supprimer un produit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produit supprimé avec succès"),
            @ApiResponse(responseCode = "404", description = "Produit non trouvé")
    })
    @DeleteMapping("Produits/{produitId}")
    public ResponseEntity<?> deleteProduit(@PathVariable Long produitId){
        if (!produitRepository.existsById(produitId)) {
            return ResponseEntity.notFound().build();
        }
        produitRepository.deleteById(produitId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Mettre à jour un produit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produit mis à jour avec succès"),
            @ApiResponse(responseCode = "404", description = "Produit non trouvé")
    })
    @PutMapping("Produits/{id}")
    public ResponseEntity<?> updateProduit(@PathVariable Long id, @RequestBody Produit produit){
        if (!produitRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        produit.setId(id);
        Produit produit1 = produitRepository.save(produit);
        return ResponseEntity.ok(produit1);
    }
    
    @Operation(summary = "Rechercher des produits")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Résultats de recherche"),
    })
    @GetMapping("Produits/search")
    public ResponseEntity<List<Produit>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {
        
        List<Produit> results;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            results = produitRepository.findByNomContainingOrDescriptionContainingOrCodeBarContainingOrReferenceContaining(
                    keyword, keyword, keyword, keyword);
        } else if (categoryId != null) {
            results = produitRepository.findByCategoryId(categoryId);
        } else {
            results = produitRepository.findAll();
        }
        if (minPrice != null || maxPrice != null) {
            double minPriceValue = minPrice != null ? minPrice : 0;
            double maxPriceValue = maxPrice != null ? maxPrice : Double.MAX_VALUE;
            
            results = results.stream()
                    .filter(p -> p.getPrix() >= minPriceValue && p.getPrix() <= maxPriceValue)
                    .toList();
        }
        
        return ResponseEntity.ok(results);
    }
    
    @Operation(summary = "Obtenir les unités de mesure disponibles")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des unités de mesure"),
    })
    @GetMapping("Produits/unites")
    public ResponseEntity<List<String>> getUnitesOfMeasure() {
        List<String> unites = produitRepository.findDistinctUnites();
        return ResponseEntity.ok(unites);
    }
    @Operation(summary = "afficher par code barecode scannée ")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "lister les données du produit scannée avec succes"),@ApiResponse(responseCode = "404",description = "erreur survenue produit non trouvee ou bien veuillez scanner de nouveau")})
    @GetMapping("Produits/{barreCode}")
    public Produit getByBareCode(@PathVariable String barreCode){
        return produitRepository.findByCodeBar(barreCode).orElse(null);
    }
}