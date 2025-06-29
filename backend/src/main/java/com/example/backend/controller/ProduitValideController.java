package com.example.backend.controller;

import com.example.backend.dto.ProduitValideDTO;
import com.example.backend.model.ProduitValide;
import com.example.backend.model.Produit;
import com.example.backend.model.Zone;
import com.example.backend.model.PlanInventaire;
import com.example.backend.repository.ProduitValideRepository;
import com.example.backend.repository.ProduitRepository;
import com.example.backend.repository.ZoneRepository;
import com.example.backend.repository.PlanInventaireRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/plans")
@CrossOrigin(origins = "*")
public class ProduitValideController {

    @Autowired
    private ProduitValideRepository produitValideRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private PlanInventaireRepository planInventaireRepository;

    @GetMapping("/{planId}/produits-valides")
    public ResponseEntity<List<ProduitValide>> getProduitsValides(@PathVariable Long planId) {
        return ResponseEntity.ok(produitValideRepository.findByPlanId(planId));
    }

    @PostMapping("/{planId}/produits-valides")
    public ResponseEntity<?> validerProduit(@PathVariable Long planId, @RequestBody ProduitValideDTO dto) {
        try {
            Produit produit = produitRepository.findById(dto.getProduitId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
            
            Zone zone = zoneRepository.findById(dto.getZoneId())
                .orElseThrow(() -> new RuntimeException("Zone non trouvée"));
            
            PlanInventaire plan = planInventaireRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan non trouvé"));

            if (produitValideRepository.existsByProduitIdAndZoneIdAndPlanId(
                    dto.getProduitId(), dto.getZoneId(), planId)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Ce produit est déjà validé pour cette zone dans ce plan"));
            }
            ProduitValide produitValide = new ProduitValide();
            produitValide.setProduit(produit);
            produitValide.setZone(zone);
            produitValide.setPlan(plan);
            produitValide.setQuantiteTheorique(dto.getQuantiteTheorique());
            produitValide.setQuantiteManuelle(dto.getQuantiteManuelle());
            produitValide.setQuantiteScan(dto.getQuantiteScan());
            produitValide.setDateValidation(dto.getDateValidation());

            return ResponseEntity.ok(produitValideRepository.save(produitValide));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{planId}/produits-valides/bulk")
    @Transactional
    public ResponseEntity<?> validerProduitsBulk(
            @PathVariable Long planId,
            @RequestBody List<ProduitValideDTO> dtos) {
        try {
            PlanInventaire plan = planInventaireRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan non trouvé"));

            List<ProduitValide> produitsValides = dtos.stream()
                .map(dto -> {
                    Produit produit = produitRepository.findById(dto.getProduitId())
                        .orElseThrow(() -> new RuntimeException("Produit non trouvé: " + dto.getProduitId()));
                    
                    Zone zone = zoneRepository.findById(dto.getZoneId())
                        .orElseThrow(() -> new RuntimeException("Zone non trouvée: " + dto.getZoneId()));

                    if (produitValideRepository.existsByProduitIdAndZoneIdAndPlanId(
                            dto.getProduitId(), dto.getZoneId(), planId)) {
                        throw new RuntimeException("Produit déjà validé: " + dto.getProduitId());
                    }

                    ProduitValide produitValide = new ProduitValide();
                    produitValide.setProduit(produit);
                    produitValide.setZone(zone);
                    produitValide.setPlan(plan);
                    produitValide.setQuantiteTheorique(dto.getQuantiteTheorique());
                    produitValide.setQuantiteManuelle(dto.getQuantiteManuelle());
                    produitValide.setQuantiteScan(dto.getQuantiteScan());
                    produitValide.setDateValidation(dto.getDateValidation());
                    
                    return produitValide;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(produitValideRepository.saveAll(produitsValides));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{planId}/produits-valides")
    @Transactional
    public ResponseEntity<?> supprimerProduitsValides(@PathVariable Long planId) {
        try {
            produitValideRepository.deleteByPlanId(planId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
} 