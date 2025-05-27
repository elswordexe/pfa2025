package com.example.backend.controller;

import com.example.backend.model.*;
import com.example.backend.repository.AssignationAgentRepository;
import com.example.backend.repository.PlanInventaireRepository;
import com.example.backend.repository.UtilisateurRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.awt.print.Pageable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@CrossOrigin(origins = "*")
@Tag(name = "controller des Plans", description = "APIs des Plans")
public class PlanInventaireController {
    @Autowired
    private PlanInventaireRepository planInventaireRepository;
    @Autowired
    private AssignationAgentRepository assignationAgentRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    private static final Logger logger = LoggerFactory.getLogger(PlanInventaireController.class);

    @Operation(summary = "Lister tous les plan")
    @ApiResponses(value =
    @ApiResponse(responseCode = "200", description = "lister tous les plans"))
    @GetMapping("Plans")
    public Iterable<PlanInventaire> getAllPlans() {
        return planInventaireRepository.findAll();
    }

    @Operation(summary = "retourner un plan", description = "lister les infos du plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "lister les infos du plan avec succès"),
            @ApiResponse(responseCode = "404", description = "plan introuvables")
    })
    @GetMapping("Plans/{planId}")
    public ResponseEntity<PlanInventaire> getPlanById(@PathVariable Long planId) {
        return planInventaireRepository.findById(planId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Enregistrer un plan", description = "Ajouter nouveau plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "plan enregistré avec succès"),
            @ApiResponse(responseCode = "400", description = "Donnéesdu planr invalides")
    })
    @PostMapping("Plans/ajout")
    public ResponseEntity<?> ajouterPlan(@RequestBody PlanInventaire planInventaire) {
        PlanInventaire plan = planInventaireRepository.save(planInventaire);
        return ResponseEntity.ok(plan);
    }

    @Operation(summary = "supprimer un plan", description = "deleter un plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "plan deleted avec succès"),
            @ApiResponse(responseCode = "400", description = "introuvable")
    })
    @DeleteMapping("Plans/{planId}")
    public ResponseEntity<?> supprimerPlan(@PathVariable Long planId) {
        planInventaireRepository.deleteById(planId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "mise a jour du plan", description = "update du plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "mise a jour du plan avec succès"),
            @ApiResponse(responseCode = "400", description = "errur")
    })
    @PutMapping("Plans/{planId}")
    public ResponseEntity<?> modifierPlan(@PathVariable Long planId, @RequestBody PlanInventaire planInventaire) {
        if (!planId.equals(planInventaire.getId())) {
            return ResponseEntity.badRequest().build();
        }
        PlanInventaire plan = planInventaireRepository.save(planInventaire);
        return ResponseEntity.ok(plan);
    }

    @Operation(summary = "Enregistrer un nouvel utilisateur", description = "Crée un nouveau compte utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur enregistré avec succès"),
            @ApiResponse(responseCode = "400", description = "Données d'utilisateur invalides")
    })
    @PostMapping("Plans/{planId}/agents/{agentId}/assignations")
    public ResponseEntity<?> assignerAgents(@PathVariable Long planId,
                                            @PathVariable Long agentId,
                                            @RequestBody Zone zone,
                                            Authentication authentication) {
        try {
            Utilisateur user = utilisateurRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!(user instanceof SuperAdministrateur)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Seul un super administrateur peut assigner des agents");
            }

            PlanInventaire plan = planInventaireRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan d'inventaire non trouvé"));

            if (!plan.getZones().contains(zone)) {
                return ResponseEntity.badRequest()
                        .body("La zone spécifiée ne fait pas partie de ce plan d'inventaire");
            }

            Utilisateur agent = utilisateurRepository.findById(agentId)
                    .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

            if (!(agent instanceof AgentInventaire)) {
                return ResponseEntity.badRequest()
                        .body("L'utilisateur assigné doit être un Agent d'inventaire");
            }

            AssignationAgent assignation = new AssignationAgent();
            assignation.setPlanInventaire(plan);
            assignation.setAgent((AgentInventaire) agent);
            assignation.setZone(zone);
            assignation.setAssignePar((SuperAdministrateur) user);
            assignation.setDateAssignation(LocalDateTime.now());

            AssignationAgent savedAssignation = assignationAgentRepository.save(assignation);
            return ResponseEntity.ok(savedAssignation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "lister les assignation d un plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "lister avec succes"),
            @ApiResponse(responseCode = "400", description = "erreur")
    })
    @GetMapping("Plans/{planId}/assignations")
    public ResponseEntity<List<AssignationAgent>> getAssignations(@PathVariable Long planId) {
        return ResponseEntity.ok(assignationAgentRepository.findByPlanInventaireId(planId));
    }

    @Operation(summary = "lister les zones concernées par le plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "lister avec succes"),
            @ApiResponse(responseCode = "400", description = "erreur")
    })
    @GetMapping("Plans/{planId}/zones")
    public ResponseEntity<List<Zone>> getZonesForPlan(@PathVariable Long planId) {
        return planInventaireRepository.findById(planId)
                .map(plan -> ResponseEntity.ok(plan.getZones()))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "ajouter les zones aux plans")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ajouter des zones avec succès"),
            @ApiResponse(responseCode = "400", description = "erreur")
    })
    @PostMapping("Plans/{planId}/zones")
    public ResponseEntity<?> addZonesToPlan(@PathVariable Long planId, @RequestBody List<Zone> zones) {
        try {
            PlanInventaire plan = planInventaireRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan d'inventaire non trouvé"));

            plan.getZones().addAll(zones);
            PlanInventaire updatedPlan = planInventaireRepository.save(plan);

            return ResponseEntity.ok(updatedPlan.getZones());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Compter le nombre de plan terminer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nombre de plan terminer")
    })
    @GetMapping("Plans/countterminer")
    public ResponseEntity<Integer> getCountPlanterminer() {
        long count = planInventaireRepository.countByStatus(STATUS.Termine);
        return ResponseEntity.ok((int) count);

    }

    @Operation(summary = "Compter le nombre de plan EN_cours")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nombre de plan EN_cours")
    })
    @GetMapping("Plans/countENcours")
    public ResponseEntity<Integer> getCountPlanENcours() {
        long count = planInventaireRepository.countByStatus(STATUS.EN_cours);
        return ResponseEntity.ok((int) count);

    }

    @Operation(summary = "Compter les plans par statut")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nombre de plans par statut")
    })
    @GetMapping("Plans/countByStatus")
    public ResponseEntity<Map<String, Integer>> getCountPlansByStatus() {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("Terminé", (int) planInventaireRepository.countByStatus(STATUS.Termine));
        counts.put("En cours", (int) planInventaireRepository.countByStatus(STATUS.EN_cours));
        counts.put("Indéfini", (int) planInventaireRepository.countByStatus(STATUS.Indefini));
        return ResponseEntity.ok(counts);
    }

@Operation(summary = "Obtenir les noms et dates de modification des plans")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès"),
    @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des données")
})
@GetMapping(value = "Plans/names-dates", produces = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<List<Map<String, Object>>> getPlanNameAndDate() {
    try {
        List<PlanInventaire> plans = planInventaireRepository.findAll();
        List<Map<String, Object>> planData = plans.stream()
                .map(plan -> {
                    Map<String, Object> planInfo = new HashMap<>();
                    planInfo.put("id", plan.getId());
                    planInfo.put("nom", plan.getNom());
                    planInfo.put("date", plan.getDatecremod());
                    return planInfo;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(planData);
    } catch (Exception e) {
        logger.error("Erreur lors de la récupération des plans", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
}
}