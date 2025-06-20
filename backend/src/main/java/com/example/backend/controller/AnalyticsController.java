package com.example.backend.controller;

import com.example.backend.model.*;
import com.example.backend.repository.EcartRepository;
import com.example.backend.repository.PlanInventaireRepository;
import com.example.backend.repository.ProduitRepository;
import com.example.backend.repository.ZoneRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AnalyticsController {
    @Autowired
    private ProduitRepository produitRepository;
    @Autowired
    private PlanInventaireRepository planRepository;
    @Autowired
    private ZoneRepository zoneRepository;
    @Autowired
    private EcartRepository ecartRepository;

    @GetMapping("/plans/stats")
    public ResponseEntity<List<Map<String, Object>>> getPlanStats() {
        List<Map<String, Object>> stats = new ArrayList<>();
        Map<String, Integer> completedPlans = planRepository.countByStatutGroupByMonth(STATUS.Termine);
        Map<String, Integer> totalPlans = planRepository.countAllGroupByMonth();
        
        for (String month : totalPlans.keySet()) {
            Map<String, Object> monthStat = new HashMap<>();
            monthStat.put("month", month);
            monthStat.put("completedPlans", completedPlans.getOrDefault(month, 0));
            monthStat.put("totalPlans", totalPlans.get(month));
            stats.add(monthStat);
        }
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/ecarts/stats")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getEcartsStats() {
        Map<String, List<Map<String, Object>>> stats = new HashMap<>();
        
        // Get surplus and missing products
        List<Map<String, Object>> surplus = new ArrayList<>();
        List<Map<String, Object>> manquants = new ArrayList<>();
        
        List<ZoneProduit> zoneProduits = zoneRepository.findAllZoneProduits();
        for (ZoneProduit zp : zoneProduits) {
            int difference = zp.getQuantiteReelle() - zp.getQuantiteTheorique();
            Map<String, Object> ecart = new HashMap<>();
            ecart.put("productId", zp.getProduit().getId());
            ecart.put("quantity", difference);
            ecart.put("zone", zp.getZone().getName());
            
            if (difference > 0) {
                surplus.add(ecart);
            } else if (difference < 0) {
                manquants.add(ecart);
            }
        }
        
        stats.put("surplus", surplus);
        stats.put("manquants", manquants);
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/zones/stats")
    public ResponseEntity<List<Map<String, Object>>> getZoneStats() {
        List<Map<String, Object>> stats = new ArrayList<>();
        
        List<Zone> zones = zoneRepository.findAll();
        for (Zone zone : zones) {
            Map<String, Object> zoneStat = new HashMap<>();
            zoneStat.put("zoneId", zone.getId());
            zoneStat.put("name", zone.getName());
            zoneStat.put("totalProducts", zone.getZoneProduits().size());
            zoneStat.put("lastInventoryDate", getLastInventoryDate(zone));
            zoneStat.put("accuracy", calculateZoneAccuracy(zone));
            stats.add(zoneStat);
        }
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/products/stats")
    public ResponseEntity<List<Map<String, Object>>> getProductStats() {
        List<Map<String, Object>> stats = new ArrayList<>();
        
        List<Produit> products = produitRepository.findAll();
        for (Produit product : products) {
            Map<String, Object> productStat = new HashMap<>();
            productStat.put("productId", product.getId());
            productStat.put("name", product.getNom());
            productStat.put("ecartCount", countEcarts(product));
            productStat.put("lastInventoryDate", getLastInventoryDate(product));
            productStat.put("quantityTheoric", product.getQuantitetheo());
            productStat.put("quantityReal", calculateRealQuantity(product));
            stats.add(productStat);
        }
        
        return ResponseEntity.ok(stats);
    }
    
    private LocalDateTime getLastInventoryDate(Zone zone) {
        return planRepository.findLastInventoryDateForZone(zone.getId());
    }
    
    private double calculateZoneAccuracy(Zone zone) {
        List<ZoneProduit> zoneProduits = (List<ZoneProduit>) zone.getZoneProduits();
        if (zoneProduits.isEmpty()) return 100.0;
        
        int totalProducts = zoneProduits.size();
        int correctProducts = 0;
        
        for (ZoneProduit zp : zoneProduits) {
            if (zp.getQuantiteReelle() == zp.getQuantiteTheorique()) {
                correctProducts++;
            }
        }
        
        return (correctProducts * 100.0) / totalProducts;
    }
    
    private int countEcarts(Produit product) {
        return zoneRepository.countEcartsForProduct(product.getId());
    }
    
    private LocalDateTime getLastInventoryDate(Produit product) {
        return planRepository.findLastInventoryDateForProduct(product.getId());
    }
    
    private int calculateRealQuantity(Produit product) {
        return zoneRepository.sumRealQuantityForProduct(product.getId());
    }
}