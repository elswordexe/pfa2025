package com.example.backend.controller;

import com.example.backend.model.Category;
import com.example.backend.model.Produit;
import com.example.backend.model.Zone;
import com.example.backend.repository.ProduitRepository;
import com.example.backend.repository.ZoneRepository;
import com.example.backend.repository.CategorieRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
@Tag(name = "Dashboard", description = "APIs pour les données du tableau de bord")
public class DashboardController {

    @Autowired
    private ProduitRepository produitRepository;
    
    @Autowired
    private ZoneRepository zoneRepository;
    
    @Autowired
    private CategorieRepository categorieRepository;

    @Operation(summary = "Obtenir le stock par catégorie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Données de stock par catégorie récupérées avec succès")
    })
    @GetMapping("/stocks/by-category")
    public ResponseEntity<List<Map<String, Object>>> getStockByCategory() {
        List<Produit> allProducts = produitRepository.findAll();
        
        // Group products by category and count
        Map<String, Integer> categoryCounts = new HashMap<>();
        
        for (Produit produit : allProducts) {
            if (produit.getCategory() != null) {
                String categoryName = produit.getCategory().getName();
                categoryCounts.put(categoryName, categoryCounts.getOrDefault(categoryName, 0) + 1);
            }
        }
        
        // Convert to format expected by frontend
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("category", entry.getKey());
            item.put("value", entry.getValue());
            result.add(item);
        }
        
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Obtenir la tendance des inventaires en temps réel")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Données de tendance des inventaires récupérées avec succès")
    })
    @GetMapping("/inventory/trend")
    public ResponseEntity<List<Map<String, Object>>> getInventoryTrend() {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        List<Produit> allProducts = produitRepository.findAll();

        Map<String, Integer> productsByMonth = new HashMap<>();
        Map<String, Integer> scannedProductsByMonth = new HashMap<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        

        for (int i = 5; i >= 0; i--) {
            LocalDateTime month = now.minusMonths(i);
            String monthKey = month.format(formatter);
            productsByMonth.put(monthKey, 0);
            scannedProductsByMonth.put(monthKey, 0);
        }

        for (Produit produit : allProducts) {
            if (produit.getDatecremod() != null) {
                LocalDateTime productDate = produit.getDatecremod();

                if (productDate.isAfter(now.minusMonths(6))) {
                    String monthKey = productDate.format(formatter);

                    productsByMonth.put(monthKey, productsByMonth.getOrDefault(monthKey, 0) + 1);
                    

                    if (produit.getCodeBarre() != null && !produit.getCodeBarre().isEmpty()) {
                        scannedProductsByMonth.put(monthKey, scannedProductsByMonth.getOrDefault(monthKey, 0) + 1);
                    }
                }
            }
        }
        for (int i = 5; i >= 0; i--) {
            LocalDateTime month = now.minusMonths(i);
            String monthKey = month.format(formatter);
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("date", monthKey);
            monthData.put("manual", productsByMonth.getOrDefault(monthKey, 0));
            monthData.put("scanned", scannedProductsByMonth.getOrDefault(monthKey, 0));
            
            result.add(monthData);
        }
        
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Obtenir les produits les plus inventoriés en temps réel")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Données des produits les plus inventoriés récupérées avec succès")
    })
    @GetMapping("/products/top")
    public ResponseEntity<List<Map<String, Object>>> getTopProducts() {

        List<Produit> allProducts = produitRepository.findAll();
        List<Zone> allZones = zoneRepository.findAll();

        Map<Long, Integer> productZoneCounts = new HashMap<>();
        Map<Long, String> productNames = new HashMap<>();

        for (Zone zone : allZones) {
            List<Produit> zoneProducts = zone.getProduits();
            if (zoneProducts != null) {
                for (Produit produit : zoneProducts) {
                    Long productId = produit.getId();
                    productZoneCounts.put(productId, productZoneCounts.getOrDefault(productId, 0) + 1);
                    productNames.put(productId, produit.getNom());
                }
            }
        }
        for (Produit produit : allProducts) {
            Long productId = produit.getId();
            if (!productZoneCounts.containsKey(productId)) {
                productZoneCounts.put(productId, 0);
                productNames.put(productId, produit.getNom());
            }
        }
        

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : productZoneCounts.entrySet()) {
            Map<String, Object> productData = new HashMap<>();
            productData.put("id", entry.getKey());
            productData.put("name", productNames.get(entry.getKey()));
            productData.put("count", entry.getValue());
            result.add(productData);
        }

        result.sort((a, b) -> ((Integer)b.get("count")).compareTo((Integer)a.get("count")));

        if (result.size() > 10) {
            result = result.subList(0, 10);
        }
        
        return ResponseEntity.ok(result);
    }
    
    @Operation(summary = "Obtenir les statistiques générales du dashboard")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistiques générales récupérées avec succès")
    })
    @GetMapping("/stats/general")
    public ResponseEntity<Map<String, Object>> getGeneralStats() {
        Map<String, Object> stats = new HashMap<>();
        long totalProducts = produitRepository.count();
        stats.put("totalProducts", totalProducts);
        long totalCategories = categorieRepository.count();
        stats.put("totalCategories", totalCategories);
        long totalZones = zoneRepository.count();
        stats.put("totalZones", totalZones);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentProducts = produitRepository.findAll().stream()
            .filter(p -> p.getDatecremod() != null && p.getDatecremod().isAfter(thirtyDaysAgo))
            .count();
        stats.put("recentProducts", recentProducts);
        
        return ResponseEntity.ok(stats);
    }
}