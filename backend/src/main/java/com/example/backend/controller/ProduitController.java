package com.example.backend.controller;

import com.example.backend.model.Category;
import com.example.backend.model.Produit;
import com.example.backend.model.Utilisateur;
import com.example.backend.model.Zone;
import com.example.backend.repository.ProduitRepository;
import com.example.backend.repository.ZoneRepository;
import com.example.backend.service.ProduitImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin(origins = "*")
@Tag(name = "controller des Produits", description = "APIs des Produits")
public class ProduitController {
    @Autowired
    private ProduitImageService produitImageService;
    @Autowired
    ProduitRepository produitRepository;
    @Autowired
    ZoneRepository zoneRepository;
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
@Transactional
public ResponseEntity<?> registerProduit(@RequestBody Map<String, Object> requestMap) {
    try {
        Produit produit = new Produit();

        if (requestMap.containsKey("nom")) produit.setNom((String) requestMap.get("nom"));
        if (requestMap.containsKey("prix")) {
            Object prixObj = requestMap.get("prix");
            if (prixObj instanceof Number) {
                produit.setPrix(((Number) prixObj).doubleValue());
            } else if (prixObj instanceof String) {
                produit.setPrix(Double.parseDouble((String) prixObj));
            }
        }
        if (requestMap.containsKey("unite")) produit.setUnite((String) requestMap.get("unite"));
        if (requestMap.containsKey("description")) produit.setDescription((String) requestMap.get("description"));
        if (requestMap.containsKey("reference")) produit.setReference((String) requestMap.get("reference"));
        if (requestMap.containsKey("codeBar")) produit.setCodeBar((String) requestMap.get("codeBar"));
        if (requestMap.containsKey("imageUrl")) produit.setImageUrl((String) requestMap.get("imageUrl"));
        

        if (requestMap.containsKey("category") && requestMap.get("category") != null) {
            Map<String, Object> categoryMap = (Map<String, Object>) requestMap.get("category");
            if (categoryMap.containsKey("id")) {
                Category category = new Category();
                category.setId(Long.valueOf(categoryMap.get("id").toString()));
                produit.setCategory(category);
            }
        }
        produit.setZones(new ArrayList<>());
        Produit savedProduit = produitRepository.save(produit);

        List<Long> zoneIds = new ArrayList<>();

        if (requestMap.containsKey("zone") && requestMap.get("zone") != null) {
            Map<String, Object> zoneMap = (Map<String, Object>) requestMap.get("zone");
            if (zoneMap.containsKey("id")) {
                zoneIds.add(Long.valueOf(zoneMap.get("id").toString()));
            }
        }
        if (requestMap.containsKey("zones") && requestMap.get("zones") != null) {
            List<Map<String, Object>> zonesList = (List<Map<String, Object>>) requestMap.get("zones");
            for (Map<String, Object> zoneMap : zonesList) {
                if (zoneMap.containsKey("id")) {
                    zoneIds.add(Long.valueOf(zoneMap.get("id").toString()));
                }
            }
        }

        for (Long zoneId : zoneIds) {
            Optional<Zone> existingZone = zoneRepository.findById(zoneId);
            if (existingZone.isPresent()) {
                Zone actualZone = existingZone.get();
                if (actualZone.getProduits() == null) {
                    actualZone.setProduits(new ArrayList<>());
                }
                if (!actualZone.getProduits().contains(savedProduit)) {
                    actualZone.getProduits().add(savedProduit);
                }
                if (savedProduit.getZones() == null) {
                    savedProduit.setZones(new ArrayList<>());
                }
                if (!savedProduit.getZones().contains(actualZone)) {
                    savedProduit.getZones().add(actualZone);
                }

                zoneRepository.save(actualZone);
            }
        }
        savedProduit = produitRepository.save(savedProduit);
        
        return ResponseEntity.ok(savedProduit);
        
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.badRequest().body("Erreur lors de l'ajout du produit: " + e.getMessage());
    }
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
    @Operation(summary ="Obetenir nombre des produits")
    @ApiResponses(value={ @ApiResponse(responseCode = "200", description = "lister nombre des produits"),})
    @GetMapping("Produits/count")
    public Long getProduitCount(){
       return  produitRepository.count();
    }
    @Operation(summary = "Obtenir des produits par zone")
@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Liste des produits par zone"), })
@GetMapping("Produits/byZone/{id}")
public List<Produit> getProduitsByZone(@PathVariable Long id) {
    Optional<Zone> zone = zoneRepository.findById(id);
    return zone.map(Zone::getProduits).orElse(List.of());
}
    @PostMapping("Produits/{produitId}/image")
    public ResponseEntity<?> uploadProductImage(
            @PathVariable("produitId") Long produitId,
            @RequestParam("image") MultipartFile file) throws IOException {

        String response = produitImageService.uploadProductImage(produitId, file);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("Produits/{produitId}/image")
    public ResponseEntity<?> getProductImage(@PathVariable("produitId") Long produitId) {
        byte[] image = produitImageService.getProductImage(produitId);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.IMAGE_JPEG)
                .body(image);
    }
@Operation(summary = "Obtenir les noms, dates et images des produits")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès"),
        @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des données")
})
@GetMapping("Produits/names-dates")
public ResponseEntity<List<Map<String, Object>>> getProduitsNameAndDate(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    try {
        Pageable pageable = PageRequest.of(page, size);
        Page<Produit> produitPage = produitRepository.findAll(pageable);

        List<Map<String, Object>> produitsData = produitPage.getContent().stream()
                .map(produit -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", produit.getId());
                    data.put("name", produit.getNom());
                    data.put("date", produit.getDatecremod());
                    data.put("imageUrl", produit.getImageUrl());
                    if (produit.getId() != null && produit.getImageUrl() != null && !produit.getImageUrl().isEmpty()) {
                        try {
                            byte[] imageData = produitImageService.getProductImage(produit.getId());
                            String base64Image = Base64.getEncoder().encodeToString(imageData);
                            data.put("imageData", base64Image);
                        } catch (Exception e) {
                            System.err.println("Could not retrieve image for product " + produit.getId() + ": " + e.getMessage());
                        }
                    }
                    
                    return data;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(produitsData);
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(List.of());
    }
}
}