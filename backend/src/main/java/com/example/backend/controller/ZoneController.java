package com.example.backend.controller;

import com.example.backend.model.Produit;
import com.example.backend.model.Zone;
import com.example.backend.repository.ProduitRepository;
import com.example.backend.repository.ZoneRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

@RestController
@CrossOrigin(origins = "*")
@Tag(name = "controller des zones", description = "APIs des zones")
public class ZoneController {
    @Autowired
    private ZoneRepository zoneRepository;
    @Autowired
    private ProduitRepository produitRepository;
    @Operation(summary = "Lister les zones")
    @ApiResponses(value =
            @ApiResponse(responseCode = "200", description = "liste de zones")
    )
    @GetMapping("Zones")
    public Iterable<Zone> getAllZones(){
        return zoneRepository.findAll();
    }
    @Operation(summary = "crée des nouveaux zones")
    @ApiResponses(value ={@ApiResponse(responseCode = "200", description = "crée avec succes"),@ApiResponse(responseCode = "400", description = "erreur")}

    )
    @PostMapping("Zones")
    public ResponseEntity<?> addZone(@RequestBody Zone zone){
        Zone zone1 = zoneRepository.save(zone);
        return ResponseEntity.ok(zone1);
    }
    @Operation(summary = "ajouter des produits aux zones")
    @ApiResponses(value ={@ApiResponse(responseCode = "200", description = "ajouter avec succes"),@ApiResponse(responseCode = "400", description = "erreur")}

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
    @Operation(summary = "mise a jour de la zone")
    @ApiResponses(value ={@ApiResponse(responseCode = "200", description = "mise a jour avec succes"),@ApiResponse(responseCode = "400", description = "erreur")}

    )
    @PutMapping("Zones/{zoneId}")
    public ResponseEntity<?> updateZone(@PathVariable Long zoneId, @RequestBody Zone zone){
        Zone zone1 = zoneRepository.save(zone);
        return ResponseEntity.ok(zone1);
    }
    @Operation(summary = "supprimer la zone par id")
    @ApiResponses(value ={@ApiResponse(responseCode = "200", description = "supprimer la zone avec succes"),@ApiResponse(responseCode = "400", description = "erreur")}

    )
    @DeleteMapping("Zones/{zoneId}")
    public ResponseEntity<?> deleteZone(@PathVariable Long zoneId){
        zoneRepository.deleteById(zoneId);
        return ResponseEntity.ok().build();
    }
    @Operation(summary = "lister les produits d une zone spécifique ")
    @ApiResponses(value ={@ApiResponse(responseCode = "200", description = "lister avec succe"),@ApiResponse(responseCode = "400", description = "erreur")}

    )
    @GetMapping("Zones/{zoneId}/products")
    public ResponseEntity<?> getZoneProducts(@PathVariable Long zoneId) {
        Optional<Zone> zoneOpt = zoneRepository.findById(zoneId);
        
        if (zoneOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(zoneOpt.get().getProduits());
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
}