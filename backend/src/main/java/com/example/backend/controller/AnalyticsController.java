package com.example.backend.controller;

import com.example.backend.model.*;
import com.example.backend.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
@Tag(name = "Analytics", description = "API pour les statistiques et analyses")
@Slf4j
public class AnalyticsController {

    @Autowired
    private PlanInventaireRepository planInventaireRepository;
    
    @Autowired
    private CheckupRepository checkupRepository;
    
    @Autowired
    private EcartRepository ecartRepository;

    @Operation(summary = "Obtenir les statistiques globales")
    @GetMapping("/global-stats")
    public ResponseEntity<Map<String, Object>> getGlobalStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            long totalPlans = planInventaireRepository.count();
            long plansEnCours = planInventaireRepository.countByStatut(STATUS.EN_cours);
            long plansTermines = planInventaireRepository.countByStatut(STATUS.Termine);
            
            stats.put("totalPlans", totalPlans);
            stats.put("plansEnCours", plansEnCours);
            stats.put("plansTermines", plansTermines);
            stats.put("tauxCompletion", totalPlans > 0 ? (double) plansTermines / totalPlans * 100 : 0);

            List<Checkup> allCheckups = checkupRepository.findAll();
            long totalCheckups = allCheckups.size();
            long checkupsAvecRecomptage = allCheckups.stream()
                .filter(c -> c.isDemandeRecomptage())
                .count();

            stats.put("totalCheckups", totalCheckups);
            stats.put("checkupsAvecRecomptage", checkupsAvecRecomptage);
            stats.put("tauxRecomptage", totalCheckups > 0 ? (double) checkupsAvecRecomptage / totalCheckups * 100 : 0);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques globales", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @Operation(summary = "Obtenir les statistiques des écarts")
    @GetMapping("/ecarts-stats")
    public ResponseEntity<Map<String, Object>> getEcartsStats() {
        try {
            List<Ecart> ecarts = ecartRepository.findAll();
            Map<String, Object> stats = new HashMap<>();
            Map<EcartType, Long> ecartsByType = ecarts.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    Ecart::getType,
                    java.util.stream.Collectors.counting()
                ));

            Map<EcartStatut, Long> ecartsByStatut = ecarts.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    Ecart::getStatut,
                    java.util.stream.Collectors.counting()
                ));

            stats.put("totalEcarts", ecarts.size());
            stats.put("ecartsByType", ecartsByType);
            stats.put("ecartsByStatut", ecartsByStatut);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques des écarts", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @Operation(summary = "Obtenir les statistiques de performance")
    @GetMapping("/performance-stats")
    public ResponseEntity<Map<String, Object>> getPerformanceStats() {
        try {
            List<PlanInventaire> plans = planInventaireRepository.findAll();
            Map<String, Object> stats = new HashMap<>();

            double avgCompletionTime = plans.stream()
                .filter(p -> p.getDateDebut() != null && p.getDateFin() != null)
                .mapToLong(p -> ChronoUnit.HOURS.between(p.getDateDebut(), p.getDateFin()))
                .average()
                .orElse(0.0);

            Map<Long, Double> recomptagePlanStats = new HashMap<>();
            for (PlanInventaire plan : plans) {
                List<Checkup> planCheckups = checkupRepository.findByPlanId(plan.getId());
                if (!planCheckups.isEmpty()) {
                    long recomptages = planCheckups.stream()
                        .filter(c -> c.isDemandeRecomptage())
                        .count();
                    recomptagePlanStats.put(plan.getId(), 
                        (double) recomptages / planCheckups.size() * 100);
                }
            }

            stats.put("avgCompletionTimeHours", avgCompletionTime);
            stats.put("recomptagePlanStats", recomptagePlanStats);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques de performance", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @Operation(summary = "Obtenir les statistiques temporelles")
    @GetMapping("/time-stats")
    public ResponseEntity<Map<String, Object>> getTimeStats(
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate
    ) {
        try {
            if (startDate == null) {
                startDate = LocalDateTime.now().minusMonths(1);
            }
            if (endDate == null) {
                endDate = LocalDateTime.now();
            }

            List<PlanInventaire> plans = planInventaireRepository.findByDateCreationBetween(startDate, endDate);
            List<Checkup> checkups = checkupRepository.findByDateCheckBetween(startDate, endDate);
            List<Ecart> ecarts = ecartRepository.findByDateCreationBetween(startDate, endDate);

            Map<String, Object> stats = new HashMap<>();

            stats.put("nombrePlans", plans.size());
            stats.put("nombreCheckups", checkups.size());
            stats.put("nombreEcarts", ecarts.size());

            Map<String, Long> plansParJour = plans.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    p -> p.getDateCreation().toLocalDate().toString(),
                    java.util.stream.Collectors.counting()
                ));

            Map<String, Long> checkupsParJour = checkups.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    c -> c.getDateCheck().toLocalDate().toString(),
                    java.util.stream.Collectors.counting()
                ));

            stats.put("evolutionPlans", plansParJour);
            stats.put("evolutionCheckups", checkupsParJour);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques temporelles", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}