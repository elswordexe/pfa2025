package com.example.backend.controller;

import com.example.backend.model.Produit;
import com.example.backend.model.Zone;
import com.example.backend.repository.ProduitRepository;
import com.example.backend.repository.ZoneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

@RestController
@CrossOrigin(origins = "*")
public class ZoneController {
    @Autowired
    private ZoneRepository zoneRepository;
    @Autowired
    private ProduitRepository produitRepository;
    @GetMapping("Zones")
    public Iterable<Zone> getAllZones(){
        return zoneRepository.findAll();
    }

    @PostMapping("Zones/add")
    public ResponseEntity<?> addZone(@RequestBody Zone zone){
        Zone zone1 = zoneRepository.save(zone);
        return ResponseEntity.ok(zone1);
    }

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
    @PutMapping("Zones/{zoneId}")
    public ResponseEntity<?> updateZone(@PathVariable Long zoneId, @RequestBody Zone zone){
        Zone zone1 = zoneRepository.save(zone);
        return ResponseEntity.ok(zone1);
    }
    @DeleteMapping("Zones/{zoneId}")
    public ResponseEntity<?> deleteZone(@PathVariable Long zoneId){
        zoneRepository.deleteById(zoneId);
        return ResponseEntity.ok().build();
    }
    @GetMapping("Zones/{zoneId}/products")
    public ResponseEntity<?> getZoneProducts(@PathVariable Long zoneId) {
        Optional<Zone> zoneOpt = zoneRepository.findById(zoneId);
        
        if (zoneOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(zoneOpt.get().getProduits());
    }

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