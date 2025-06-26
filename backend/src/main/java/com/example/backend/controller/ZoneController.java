package com.example.backend.controller;

import com.example.backend.model.Produit;
import com.example.backend.model.Zone;
import com.example.backend.model.ZoneProduitId;
import com.example.backend.repository.ProduitRepository;
import com.example.backend.repository.ZoneRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.stream.Collectors;
import com.example.backend.dto.ProduitDTO;
import com.example.backend.dto.ZoneDTO;
import com.example.backend.dto.ZoneProduitDTO;
import com.example.backend.model.ZoneProduit;

@RestController
@CrossOrigin(origins = "*")
@Tag(name = "controller des zones", description = "APIs des zones")
public class ZoneController {
    @Autowired
    private ZoneRepository zoneRepository;
    @Autowired
    private ProduitRepository produitRepository;
    @Operation(summary = "Lister les zones")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "liste de zones",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Zone.class))))
    })
    @GetMapping("Zone/all")
    public Iterable<Zone> getAllZones(){
        return zoneRepository.findAll();
    }
    @Operation(
            summary = "ajouter des produits aux zones",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Liste d'IDs de produits à associer à la zone",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Long.class),
                            examples = @ExampleObject(
                                    name = "Exemple d'IDs de produits",
                                    value = "[1, 2, 3]"
                            )
                    )
            )
    )
    @PostMapping("Zones/{zoneId}")
    @Transactional
    public ResponseEntity<?> addProductsToZone(@PathVariable Long zoneId, @RequestBody List<Long> productIds) {
        Optional<Zone> zoneOpt = zoneRepository.findById(zoneId);
        if (zoneOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Zone zone = zoneOpt.get();
        List<Produit> existingProducts = produitRepository.findAllById(productIds);
        
        if (existingProducts.isEmpty()) {
            return ResponseEntity.badRequest().body("Aucun produit valide trouvé");
        }
        
        if (existingProducts.size() < productIds.size()) {
            return ResponseEntity.badRequest().body("Certains produits n'ont pas été trouvés");
        }
        Set<Produit> currentProducts = new HashSet<>(zone.getProduits());
        for (Produit product : existingProducts) {
            if (currentProducts.add(product)) {
                product.getZones().add(zone);
            }
        }
        zone.setProduits(new ArrayList<>(currentProducts));
        
        Zone updatedZone = zoneRepository.save(zone);
        return ResponseEntity.ok(updatedZone);
    }

    @Operation(summary = "supprimer la zone par id")
    @ApiResponses(value ={@ApiResponse(responseCode = "200", description = "supprimer la zone avec succes"),@ApiResponse(responseCode = "400", description = "erreur")}

    )
    @DeleteMapping("Zones/{zoneId}")
    public ResponseEntity<?> deleteZone(@PathVariable Long zoneId){
        return zoneRepository.findById(zoneId)
            .map(zone -> {
                zoneRepository.delete(zone); // la suppression en cascade gère les relations
                return ResponseEntity.ok().build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "lister les produits d une zone spécifique ")
    @ApiResponses(value ={
            @ApiResponse(responseCode = "200", description = "lister avec succes",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProduitDTO.class)))),
            @ApiResponse(responseCode = "400", description = "erreur")
    })
    @GetMapping("Zones/{zoneId}/products")
    public ResponseEntity<?> getZoneProducts(@PathVariable Long zoneId) {
        Optional<Zone> zoneOpt = zoneRepository.findById(zoneId);
        
        if (zoneOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Zone zone = zoneOpt.get();
        List<ProduitDTO> produitDTOs = zone.getProduits().stream()
            .map(produit -> {
                ProduitDTO dto = new ProduitDTO();
                dto.setId(produit.getId());
                dto.setCodeBarre(produit.getCodeBarre());
                dto.setReference(produit.getReference());
                dto.setNom(produit.getNom());
                dto.setDescription(produit.getDescription());
                dto.setPrix(produit.getPrix());
                dto.setUnite(produit.getUnite());
                dto.setDatecremod(produit.getDatecremod());
                dto.setImageUrl(produit.getImageUrl());
                dto.setQuantitetheo(produit.getQuantitetheo());
                if (produit.getCategory() != null) {
                    ProduitDTO.CategoryDTO categoryDTO = new ProduitDTO.CategoryDTO();
                    categoryDTO.setId(produit.getCategory().getId());
                    categoryDTO.setName(produit.getCategory().getName());
                    dto.setCategory(categoryDTO);
                }
                if (produit.getSubCategory() != null) {
                    ProduitDTO.SubCategoryDTO subCategoryDTO = new ProduitDTO.SubCategoryDTO();
                    subCategoryDTO.setId(produit.getSubCategory().getId());
                    subCategoryDTO.setName(produit.getSubCategory().getName());
                    dto.setSubCategory(subCategoryDTO);
                }
                
                return dto;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(produitDTOs);
    }
    @Operation(summary = "supprimer un produit spécifique de le zone choisie")
    @ApiResponses(value ={@ApiResponse(responseCode = "200", description = "suppression du produit avec succes"),@ApiResponse(responseCode = "400", description = "erreur")}

    )
    @DeleteMapping("Zones/{zoneId}/products/{productId}")
    public ResponseEntity<?> removeProductFromZone(@PathVariable Long zoneId, @PathVariable Long productId) {
        Optional<Zone> zoneOpt = zoneRepository.findById(zoneId);
        if (zoneOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Zone zone = zoneOpt.get();
        zone.setProduits(zone.getProduits().stream()
                .filter(p -> !p.getId().equals(productId))
                .toList());
        
        Zone updatedZone = zoneRepository.save(zone);
        return ResponseEntity.ok(updatedZone);
    }
    @GetMapping("Zone/count")
    public Long getZoneCount(){
        return zoneRepository.count();
    }
    @PutMapping("/Zone/update/{id}")
    @Operation(
            summary = "Mettre à jour une zone",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Données de la zone à mettre à jour",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ZoneDTO.class),
                            examples = @ExampleObject(
                                    name = "Exemple de zone",
                                    value = "{\n  \"name\": \"Zone A\",\n  \"description\": \"Zone de stockage principale\",\n  \"zoneProduits\": [ { \"produitId\": 1, \"quantitetheo\": 100 } ]\n}"
                            )
                    )
            )
    )
    public ResponseEntity<?> updateZone(@PathVariable Long id, @RequestBody ZoneDTO zoneDTO) {
        Optional<Zone> existingZoneOpt = zoneRepository.findById(id);
        if (existingZoneOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Zone existingZone = existingZoneOpt.get();
        existingZone.setName(zoneDTO.getName());
        existingZone.setDescription(zoneDTO.getDescription());
        existingZone.getZoneProduits().clear();
        if (zoneDTO.getZoneProduits() != null) {
            for (ZoneProduitDTO zpDTO : zoneDTO.getZoneProduits()) {
                Optional<Produit> produitOpt = produitRepository.findById(zpDTO.getProduitId());
                if (produitOpt.isPresent()) {
                    Produit produit = produitOpt.get();
                    Integer requested = zpDTO.getQuantitetheo() == null ? 0 : zpDTO.getQuantitetheo();
                    Integer totalDisponible = produit.getQuantitetheo();
                    if (totalDisponible != null) {
                        int dejaAlloue = produit.getZoneProduits().stream()
                                .filter(zp -> !zp.getZone().getId().equals(id))
                                .map(ZoneProduit::getQuantiteTheorique)
                                .filter(q -> q != null)
                                .mapToInt(Integer::intValue)
                                .sum();
                        int restant = totalDisponible - dejaAlloue;
                        if (requested > restant) {
                            return ResponseEntity.badRequest().body("La quantité demandée pour le produit " + produit.getNom() + " dépasse la quantité restante disponible (" + restant + ")");
                        }
                    }

                    ZoneProduit zoneProduit = new ZoneProduit();
                    zoneProduit.setId(new ZoneProduitId(id, zpDTO.getProduitId()));
                    zoneProduit.setZone(existingZone);
                    zoneProduit.setProduit(produit);
                    zoneProduit.setQuantiteTheorique(zpDTO.getQuantitetheo());
                    existingZone.getZoneProduits().add(zoneProduit);
                }
            }
        }

        Zone updatedZone = zoneRepository.save(existingZone);
        return ResponseEntity.ok(updatedZone);
    }

    @PatchMapping("/Zone/update/{id}")
    public ResponseEntity<?> patchUpdateZone(@PathVariable Long id, @RequestBody ZoneDTO zoneDTO) {
        return updateZone(id, zoneDTO);
    }

    @PostMapping("Zone")
    @Operation(
            summary = "Créer une zone",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Données de la zone à créer",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ZoneDTO.class),
                            examples = @ExampleObject(
                                    name = "Exemple de création de zone",
                                    value = "{\n  \"name\": \"Zone B\",\n  \"description\": \"Zone réfrigérée\",\n  \"zoneProduits\": []\n}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Zone créée",
                    content = @Content(schema = @Schema(implementation = Zone.class)))
    })
    public ResponseEntity<?> createZone(@RequestBody ZoneDTO zoneDTO) {
        Zone newZone = new Zone();
        newZone.setName(zoneDTO.getName());
        newZone.setDescription(zoneDTO.getDescription());

        if (zoneDTO.getZoneProduits() != null) {
            for (ZoneProduitDTO zpDTO : zoneDTO.getZoneProduits()) {
                Optional<Produit> produitOpt = produitRepository.findById(zpDTO.getProduitId());
                if (produitOpt.isPresent()) {
                    Produit produit = produitOpt.get();
                    Integer requested = zpDTO.getQuantitetheo() == null ? 0 : zpDTO.getQuantitetheo();
                    Integer totalDisponible = produit.getQuantitetheo();
                    if (totalDisponible != null) {
                        int dejaAlloue = produit.getZoneProduits().stream()
                                .map(ZoneProduit::getQuantiteTheorique)
                                .filter(q -> q != null)
                                .mapToInt(Integer::intValue)
                                .sum();
                        int restant = totalDisponible - dejaAlloue;
                        if (requested > restant) {
                            return ResponseEntity.badRequest().body("La quantité demandée pour le produit " + produit.getNom() + " dépasse la quantité restante disponible (" + restant + ")");
                        }
                    }

                    ZoneProduit zoneProduit = new ZoneProduit();
                    zoneProduit.setZone(newZone);
                    zoneProduit.setProduit(produit);
                    zoneProduit.setQuantiteTheorique(zpDTO.getQuantitetheo());
                    newZone.getZoneProduits().add(zoneProduit);
                }
            }
        }

        Zone savedZone = zoneRepository.save(newZone);
        return ResponseEntity.ok(savedZone);
    }
}