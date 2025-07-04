
package com.example.backend.controller;

import com.example.backend.dto.PlanInventaireDTO;
import com.example.backend.model.*;
import com.example.backend.repository.*;
import com.example.backend.model.ZoneProduitPlanValide;
import com.example.backend.repository.ZoneProduitPlanValideRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.example.backend.model.AdministrateurClient;

@RestController
@RequestMapping("/api/plans")
@CrossOrigin(origins = "*")
@Tag(name = "Plan d'Inventaire", description = "API de gestion des plans d'inventaire")
@Slf4j
@RequiredArgsConstructor

public class PlanInventaireController {

    private final PlanInventaireRepository planInventaireRepository;
    private final ProduitRepository produitRepository;
    private final ZoneRepository zoneRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AssignationAgentRepository assignationAgentRepository;
    private final EcartRepository ecartRepository;
    private final CheckupRepository checkupRepository;
    private final ZoneProduitPlanValideRepository zoneProduitPlanValideRepository;
    private final ZoneProduitRepository zoneProduitRepository;
    @PutMapping("/{planId}/produits/{produitId}/zones/{zoneId}/valider")
    public ResponseEntity<?> validerProduitPourPlanZoneNouveau(@PathVariable Long planId,
                                                              @PathVariable Long produitId,
                                                              @PathVariable Long zoneId,
                                                              @RequestBody(required = false) Map<String, Object> body,
                                                              Authentication authentication) {
        try {
            PlanInventaire plan = planInventaireRepository.findById(planId)
                    .orElseThrow(() -> new EntityNotFoundException("Plan non trouvé avec l'id: " + planId));
            Produit produit = produitRepository.findById(produitId)
                    .orElseThrow(() -> new EntityNotFoundException("Produit non trouvé avec l'id: " + produitId));
            Zone zone = zoneRepository.findById(zoneId)
                    .orElseThrow(() -> new EntityNotFoundException("Zone non trouvée avec l'id: " + zoneId));

            Integer quantiteValidee = null;
            if (body != null && body.containsKey("quantiteValidee")) {
                Object q = body.get("quantiteValidee");
                if (q instanceof Number) {
                    quantiteValidee = ((Number) q).intValue();
                } else if (q instanceof String) {
                    try {
                        quantiteValidee = Integer.parseInt((String) q);
                    } catch (NumberFormatException ignored) {}
                }
            }

            Long userId = null;
            if (authentication != null) {
                String email = authentication.getName();
                Utilisateur user = utilisateurRepository.findByEmail(email).orElse(null);
                if (user != null) userId = user.getId();
            }
            ZoneProduitPlanValide zpValide = zoneProduitPlanValideRepository.findByPlanAndZoneAndProduit(plan, zone, produit)
                    .orElse(null);

            ZoneProduit zoneProduit = null;
            if (zone.getZoneProduits() != null) {
                for (ZoneProduit zp : zone.getZoneProduits()) {
                    if (zp.getProduit() != null && zp.getProduit().getId().equals(produitId)) {
                        zoneProduit = zp;
                        break;
                    }
                }
            }
            Integer oldQuantite = zoneProduit != null ? zoneProduit.getQuantiteTheorique() : null;

            if (zpValide == null) {
                zpValide = ZoneProduitPlanValide.builder()
                        .plan(plan)
                        .zone(zone)
                        .produit(produit)
                        .quantiteValidee(quantiteValidee)
                        .dateValidation(java.time.LocalDateTime.now())
                        .validatedByUserId(userId)
                        .oldQuantiteAvant(oldQuantite)
                        .build();
            } else {
                zpValide.setQuantiteValidee(quantiteValidee);
                zpValide.setDateValidation(java.time.LocalDateTime.now());
                zpValide.setValidatedByUserId(userId);
                if (zpValide.getOldQuantiteAvant() == null) {
                    zpValide.setOldQuantiteAvant(oldQuantite);
                }
            }

            zoneProduitPlanValideRepository.save(zpValide);
            if (zoneProduit != null && quantiteValidee != null) {
                zoneProduit.setQuantiteTheorique(quantiteValidee);
                zoneProduitRepository.save(zoneProduit);
                if (produit.getQuantitetheo() != null && quantiteValidee > produit.getQuantitetheo()) {
                    produit.setQuantitetheo(quantiteValidee);
                    produitRepository.save(produit);
                }
            }

            boolean allValidated = true;
            for (Produit p : plan.getProduits()) {
                for (Zone z : plan.getZones()) {
                    boolean hasZoneProduit = false;
                    if (z.getZoneProduits() != null) {
                        for (ZoneProduit zp : z.getZoneProduits()) {
                            if (zp.getProduit() != null && zp.getProduit().getId().equals(p.getId())) {
                                hasZoneProduit = true;
                                ZoneProduitPlanValide zpv = zoneProduitPlanValideRepository.findByPlanAndZoneAndProduit(plan, z, p).orElse(null);
                                if (zpv == null) {
                                    allValidated = false;
                                    break;
                                }
                            }
                        }
                    }
                    if (!hasZoneProduit) continue;
                    if (!allValidated) break;
                }
                if (!allValidated) break;
            }
            if (allValidated) {
                plan.setStatut(STATUS.Termine);
            } else {
                plan.setStatut(STATUS.EN_cours);
            }
            planInventaireRepository.save(plan);

            return ResponseEntity.ok(Map.of("message", "Produit validé pour le plan et la zone (nouveau système)", "id", zpValide.getId()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Lister tous les plan")
    @ApiResponses(value =
    @ApiResponse(responseCode = "200", description = "lister tous les plans"))
    @GetMapping
    @Transactional
    public ResponseEntity<List<PlanInventaireDTO>> getAllPlans() {
        try {
            log.info("Récupération de tous les plans avec leurs détails");
            List<PlanInventaire> plans = planInventaireRepository.findAllWithDetails();
            log.info("Nombre de plans trouvés: {}", plans.size());

            List<PlanInventaireDTO> dtos = new ArrayList<>();
            for (PlanInventaire plan : plans) {
                try {
                    PlanInventaireDTO dto = convertToDTO(plan);
                    dtos.add(dto);
                    log.info("Plan {} converti avec succès", plan.getId());
                } catch (Exception e) {
                    log.error("Erreur lors de la conversion du plan {}: {}", plan.getId(), e.getMessage());
                }
            }
            
            log.info("Conversion de tous les plans terminée. Nombre de DTOs: {}", dtos.size());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des plans: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la récupération des plans: " + e.getMessage());
        }
    }

    @Operation(summary = "retourner un plan", description = "lister les infos du plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "lister les infos du plan avec succès"),
            @ApiResponse(responseCode = "404", description = "plan introuvables")
    })
    @GetMapping("{planId}")
    @Transactional
    public ResponseEntity<PlanInventaireDTO> getPlanById(@PathVariable Long planId) {
        return planInventaireRepository.findById(planId)
                .map(plan -> ResponseEntity.ok(convertToDTO(plan)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Créer un plan d'inventaire",
        description = "Créer un nouveau plan avec nom, dates, statut, zones, produits et type.\nChamps attendus :\n- nom (String)\n- dateDebut (String, format ISO)\n- dateFin (String, format ISO)\n- type (String: COMPLET|PARTIEL)\n- recurrence (String: MENSUEL|ANNUEL)\n- statut (String: Planifie|EN_cours|Termine|Indefini)\n- createur (objet: { id: Long, dtype: String (ADMIN_CLIENT|SUPER_ADMIN|...)} )\n- zones (array d'objets: { id: Long })\n- produits (array d'objets: { id: Long }) (optionnel pour COMPLET)",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Exemple de corps de requête pour créer un plan d'inventaire. Le champ 'createur' doit contenir l'id et le dtype (type d'utilisateur, ex: ADMIN_CLIENT, SUPER_ADMIN, etc.)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    type = "object",
                    example = "{\n  \"nom\": \"Inventaire trimestriel\",\n  \"dateDebut\": \"2025-01-01T08:00:00\",\n  \"dateFin\": \"2025-01-10T18:00:00\",\n  \"type\": \"COMPLET\",\n  \"recurrence\": \"MENSUEL\",\n  \"statut\": \"Planifie\",\n  \"createur\": { \"id\": 1, \"dtype\": \"ADMIN_CLIENT\" },\n  \"zones\": [ { \"id\": 1 }, { \"id\": 2 } ],\n  \"produits\": [ { \"id\": 10 }, { \"id\": 11 } ]\n}"
                )
            )
        ),
        responses = {
            @ApiResponse(responseCode = "201", description = "Plan d'inventaire créé",
                content = @Content(schema = @Schema(
                    type = "object",
                    example = "{\n  \"id\": 1,\n  \"message\": \"Plan d'inventaire créé avec succès\"\n}"
                )))
        }
    )
    @PostMapping
    public ResponseEntity<Map<String, Object>> createPlan(@RequestBody Map<String, Object> requestData) {
        try {
            PlanInventaire plan = new PlanInventaire();
            plan.setNom((String) requestData.get("nom"));
            plan.setDateDebut(parseDateTime((String) requestData.get("dateDebut")));
            plan.setDateFin(parseDateTime((String) requestData.get("dateFin")));
            plan.setType(TYPE.valueOf((String) requestData.get("type")));
            String recurrenceStr = (String) requestData.get("recurrence");
            if (recurrenceStr == null || recurrenceStr.isBlank()) {
                plan.setRecurrence(RECCURENCE.MENSUEL);
            } else {
                plan.setRecurrence(RECCURENCE.valueOf(recurrenceStr));
            }

            String statutStr = (String) requestData.get("statut");
            plan.setStatut(statutStr != null ? STATUS.valueOf(statutStr) : STATUS.Indefini);
            plan.setDateCreation(LocalDateTime.now());
            Map<String, Object> createurMap = (Map<String, Object>) requestData.get("createur");
            if (createurMap != null) {
                Long createurId = ((Number) createurMap.get("id")).longValue();
                Utilisateur createur = utilisateurRepository.findById(createurId)
                        .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable avec l'id: " + createurId));
                plan.setCreateur(createur);
            } else {
                throw new IllegalArgumentException("Champ 'createur' manquant dans la requête");
            }
            List<Map<String, Object>> zonesData = (List<Map<String, Object>>) requestData.get("zones");
            Set<Zone> zones = new HashSet<>();
            Set<Produit> produits = new HashSet<>();

            if (zonesData != null) {
                for (Map<String, Object> zoneData : zonesData) {
                    Long zoneId = ((Number) zoneData.get("id")).longValue();
                    Zone zone = zoneRepository.findById(zoneId)
                            .orElseThrow(() -> new EntityNotFoundException("Zone not found with id: " + zoneId));
                    zones.add(zone);
                }
                plan.setZones(zones);
            }

            List<Map<String, Object>> produitsData = (List<Map<String, Object>>) requestData.get("produits");
            
            if (plan.getType() == TYPE.COMPLET) {
                for (Zone zone : zones) {
                    produits.addAll(zone.getProduits());
                }
                plan.setInclusTousProduits(true);
            } else if (plan.getType() == TYPE.PARTIEL) {
                if (produitsData != null) {
                    for (Map<String, Object> produitData : produitsData) {
                        Long produitId = ((Number) produitData.get("id")).longValue();
                        Produit produit = produitRepository.findById(produitId)
                                .orElseThrow(() -> new EntityNotFoundException("Produit not found with id: " + produitId));
                        boolean produitInSelectedZones = zones.stream()
                                .anyMatch(zone -> zone.getProduits().contains(produit));
                        
                        if (!produitInSelectedZones) {
                            throw new IllegalArgumentException("Produit with id " + produitId + " does not belong to any of the selected zones");
                        }
                        
                        produits.add(produit);
                    }
                }
                plan.setInclusTousProduits(false);
            }
            plan.setProduits(produits);
            PlanInventaire savedPlan = planInventaireRepository.save(plan);
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedPlan.getId());
            response.put("message", "Plan d'inventaire créé avec succès");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Erreur lors de la création du plan: " + e.getMessage()));
        }
    }
    @Operation(summary = "supprimer un plan", description = "deleter un plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "plan deleted avec succès"),
            @ApiResponse(responseCode = "400", description = "introuvable")
    })
    @DeleteMapping("{planId}")
    public ResponseEntity<?> supprimerPlan(@PathVariable Long planId) {
        PlanInventaire plan = planInventaireRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan introuvable"));
        List<Checkup> planCheckups = checkupRepository.findByPlanId(planId);
        checkupRepository.deleteAll(planCheckups);
        List<AssignationAgent> assigns = assignationAgentRepository.findByPlanInventaireId(planId);
        assignationAgentRepository.deleteAll(assigns);
        planInventaireRepository.delete(plan);

        return ResponseEntity.ok().build();
    }
    @Operation(summary = "Assigner un agent", description = "Assigner un plan à un agent")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Agent assigné avec succès",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PlanInventaireDTO.AssignationAgentDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Échec d'assignation"),
            @ApiResponse(responseCode = "403", description = "Accès non autorisé")
    })
    @PostMapping("{planId}/agents/{agentId}/assignations")
    public ResponseEntity<?> assignerAgents(
            @Parameter(description = "ID du plan d'inventaire") @PathVariable Long planId,
            @Parameter(description = "ID de l'agent à assigner") @PathVariable Long agentId,
            @Parameter(description = "Zone à assigner (optionnel). Si non fourni, toutes les zones du plan seront assignées",
                    required = false,
                    schema = @Schema(implementation = Zone.class))
            @RequestBody(required = false) Zone zone,
            Authentication authentication)
    {
        try {
            Utilisateur user = utilisateurRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!(user instanceof SuperAdministrateur) && !(user instanceof AdministrateurClient)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Vous n'avez pas les droits suffisants pour assigner des agents à un plan d'inventaire");
            }
            PlanInventaire plan = planInventaireRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan d'inventaire non trouvé"));

            Utilisateur agent = utilisateurRepository.findById(agentId)
                    .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

            if (!(agent instanceof AgentInventaire)) {
                return ResponseEntity.badRequest()
                        .body("L'utilisateur assigné doit être un Agent d'inventaire");
            }

            List<Zone> zonesToAssign;
            if (zone == null || zone.getId() == null) {
                zonesToAssign = new ArrayList<>(plan.getZones());
            } else {
                Zone zoneEntity = zoneRepository.findById(zone.getId())
                        .orElseThrow(() -> new RuntimeException("Zone non trouvée"));
                if (plan.getZones().stream().noneMatch(z -> z.getId().equals(zoneEntity.getId()))) {
                    return ResponseEntity.badRequest()
                            .body("La zone spécifiée ne fait pas partie de ce plan d'inventaire");
                }
                zonesToAssign = List.of(zoneEntity);
            }

            List<PlanInventaireDTO.AssignationAgentDTO> assignationDTOs = new ArrayList<>();

            for (Zone z : zonesToAssign) {
                AssignationAgent assignation = new AssignationAgent();
                assignation.setPlanInventaire(plan);
                assignation.setAgent((AgentInventaire) agent);
                assignation.setZone(z);
                assignation.setDateAssignation(LocalDateTime.now());

                AssignationAgent savedAssignation = assignationAgentRepository.save(assignation);

                PlanInventaireDTO.AssignationAgentDTO assignationDTO = new PlanInventaireDTO.AssignationAgentDTO();
                assignationDTO.setId(savedAssignation.getId());
                assignationDTO.setDateAssignation(savedAssignation.getDateAssignation());
                PlanInventaireDTO.ZoneDTO zoneDTO = new PlanInventaireDTO.ZoneDTO();
                zoneDTO.setId(z.getId());
                zoneDTO.setName(z.getName());
                assignationDTO.setZone(zoneDTO);
                PlanInventaireDTO.AgentInventaireDTO agentDTO = new PlanInventaireDTO.AgentInventaireDTO();
                agentDTO.setId(agent.getId());
                agentDTO.setNom(agent.getNom());
                agentDTO.setPrenom(agent.getPrenom());
                agentDTO.setLastName(agent.getNom());
                agentDTO.setFirstName(agent.getPrenom());
                agentDTO.setEmail(agent.getEmail());
                agentDTO.setRole(String.valueOf(agent.getRole()));
                assignationDTO.setAgent(agentDTO);
                assignationDTOs.add(assignationDTO);
                plan.getAssignations().add(savedAssignation);
            }
            planInventaireRepository.save(plan);

            return ResponseEntity.status(HttpStatus.CREATED).body(assignationDTOs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @Operation(summary = "lister les assignation d un plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "lister avec succes"),
            @ApiResponse(responseCode = "400", description = "erreur")
    })
    @GetMapping("{planId}/assignations")
    public ResponseEntity<List<PlanInventaireDTO.AssignationAgentDTO>> getPlanAssignations(@PathVariable Long planId) {
        List<AssignationAgent> assignations = assignationAgentRepository.findByPlanInventaireId(planId);
        List<PlanInventaireDTO.AssignationAgentDTO> dtos = assignations.stream().map(ass -> {
            PlanInventaireDTO.AssignationAgentDTO dto = new PlanInventaireDTO.AssignationAgentDTO();
            dto.setId(ass.getId());
            dto.setDateAssignation(ass.getDateAssignation());

            if (ass.getZone() != null) {
                PlanInventaireDTO.ZoneDTO z = new PlanInventaireDTO.ZoneDTO();
                z.setId(ass.getZone().getId());
                z.setName(ass.getZone().getName());
                dto.setZone(z);
            }
            if (ass.getAgent() != null) {
                PlanInventaireDTO.AgentInventaireDTO a = new PlanInventaireDTO.AgentInventaireDTO();
                a.setId(ass.getAgent().getId());
                a.setNom(ass.getAgent().getNom());
                a.setPrenom(ass.getAgent().getPrenom());
                a.setLastName(ass.getAgent().getNom());
                a.setFirstName(ass.getAgent().getPrenom());
                a.setEmail(ass.getAgent().getEmail());
                a.setRole(ass.getAgent().getRole() != null ? ass.getAgent().getRole().toString() : null);
                dto.setAgent(a);
            }
            return dto;
        }).toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "lister les zones concernées par le plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "lister avec succes"),
            @ApiResponse(responseCode = "400", description = "erreur")
    })
    @GetMapping("{planId}/zones")
    public ResponseEntity<List<Zone>> getZonesForPlan(@PathVariable Long planId) {
        return planInventaireRepository.findById(planId)
                .map(plan -> ResponseEntity.ok(plan.getZones().stream().toList()))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Ajouter les zones aux plans",
        description = "Ajoute une ou plusieurs zones à un plan d'inventaire.\nChamps attendus : id (Long) pour chaque zone.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Exemple de corps de requête pour ajouter des zones à un plan.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    type = "array",
                    example = "[ { \"id\": 1 }, { \"id\": 2 } ]"
                )
            )
        ),
        responses = {
            @ApiResponse(responseCode = "201", description = "Zones ajoutées",
                content = @Content(schema = @Schema(
                    type = "array",
                    example = "[{\"id\":1,\"name\":\"Zone A\"},{\"id\":2,\"name\":\"Zone B\"}]"
                )))
        }
    )
    @PostMapping("{planId}/zones")
    public ResponseEntity<?> addZonesToPlan(@PathVariable Long planId, @RequestBody List<Zone> zones) {
        try {
            PlanInventaire plan = planInventaireRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan d'inventaire non trouvé"));

            plan.getZones().addAll(zones);
            PlanInventaire updatedPlan = planInventaireRepository.save(plan);

            return ResponseEntity.status(HttpStatus.CREATED).body(updatedPlan.getZones());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @Operation(summary = "Compter le nombre de plan terminer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nombre de plan terminer")
    })
    @GetMapping("countterminer")
    public ResponseEntity<Integer> getCountPlanterminer() {
        long count = planInventaireRepository.countByStatut(STATUS.Termine);
        return ResponseEntity.ok((int) count);

    }

    @Operation(summary = "Compter le nombre de plan EN_cours")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nombre de plan EN_cours")
    })
    @GetMapping("countENcours")
    public ResponseEntity<Integer> getCountPlanENcours() {
        long count = planInventaireRepository.countByStatut(STATUS.EN_cours);
        return ResponseEntity.ok((int) count);

    }

    @Operation(summary = "Compter les plans par statut")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nombre de plans par statut")
    })
    @GetMapping("countByStatus")
    public ResponseEntity<Map<String, Integer>> getCountPlansByStatus() {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("Terminé", (int) planInventaireRepository.countByStatut(STATUS.Termine));
        counts.put("En cours", (int) planInventaireRepository.countByStatut(STATUS.EN_cours));
        counts.put("Indéfini", (int) planInventaireRepository.countByStatut(STATUS.Indefini));
        return ResponseEntity.ok(counts);
    }
    @Operation(summary = "Obtenir les noms et dates de modification des plans")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès"),
        @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des données")
    })
    @GetMapping(value = "names-dates", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getPlanNameAndDate() {
        try {
            List<PlanInventaire> plans = planInventaireRepository.findAll();
            List<Map<String, Object>> planData = plans.stream()
                    .map(plan -> {
                        Map<String, Object> planInfo = new HashMap<>();
                        planInfo.put("id", plan.getId());
                        planInfo.put("nom", plan.getNom());
                        planInfo.put("date", plan.getDateCreation());
                        return planInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(planData);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des plans", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @GetMapping("{planId}/ecarts")
    public ResponseEntity<List<Ecart>> getEcartsByPlan(@PathVariable Long planId) {
        List<Ecart> ecarts = ecartRepository.findByPlanInventaireId(planId);
        return ResponseEntity.ok(ecarts);
    }

    @Operation(
        summary = "Ajouter des produits au plan",
        description = "Ajoute des produits à un plan d'inventaire.\nChamps attendus : productId (Long), zoneIds (array de Long)",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Exemple de corps de requête pour ajouter des produits à un plan.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    type = "array",
                    example = "[ { \"productId\": 10, \"zoneIds\": [1,2] }, { \"productId\": 11, \"zoneIds\": [2] } ]"
                )
            )
        ),
        responses = {
            @ApiResponse(responseCode = "201", description = "Produits ajoutés avec succès",
                content = @Content(schema = @Schema(
                    type = "object",
                    example = "{\n  \"message\": \"Produits ajoutés avec succès\",\n  \"planId\": 1,\n  \"productsAdded\": 2\n}"
                )))
        }
    )
    @PostMapping("{planId}/produits")
    public ResponseEntity<?> addProductsToPlan(@PathVariable Long planId, @RequestBody List<Map<String, Object>> productsWithZones) {
        try {
            PlanInventaire plan = planInventaireRepository.findById(planId)
                    .orElseThrow(() -> new EntityNotFoundException("Plan d'inventaire non trouvé"));

        List<Produit> updatedProducts = new ArrayList<>();
        List<Zone> zonesToSave = new ArrayList<>();

            for (Map<String, Object> productData : productsWithZones) {
                Long productId = Long.valueOf(productData.get("productId").toString());
                List<Object> rawZoneIds = (List<Object>) productData.get("zoneIds");
                List<Long> zoneIds = rawZoneIds.stream()
                        .map(id -> Long.valueOf(id.toString()))
                        .collect(Collectors.toList());

                Produit produit = produitRepository.findById(productId)
                        .orElseThrow(() -> new EntityNotFoundException("Produit non trouvé: " + productId));

                for (Long zoneId : zoneIds) {
                    Zone zone = zoneRepository.findById(zoneId)
                            .orElseThrow(() -> new EntityNotFoundException("Zone non trouvée: " + zoneId));

                    if (!produit.getZones().contains(zone)) {
                        produit.getZones().add(zone);
                    }
                    if (!zone.getProduits().contains(produit)) {
                        zone.getProduits().add(produit);
                    }
                    zonesToSave.add(zone);
                }

                updatedProducts.add(produit);
                if (!plan.getProduits().contains(produit)) {
                    plan.getProduits().add(produit);
                }
            }

        zoneRepository.saveAll(zonesToSave);
        produitRepository.saveAll(updatedProducts);
        planInventaireRepository.save(plan);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of(
                "message", "Produits ajoutés avec succès",
                "planId", plan.getId(),
                "productsAdded", updatedProducts.size()
            ));
    } catch (EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", e.getMessage()));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
        log.error("Error adding products to plan", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Une erreur interne est survenue"));
    }
}

    @Operation(summary = "Retirer un produit du plan")
    @ApiResponse(responseCode = "200", description = "Produit retiré avec succès")
    @DeleteMapping("/{planId}/produits/{produitId}")
    public ResponseEntity<?> removeProduitFromPlan(@PathVariable Long planId, @PathVariable Long produitId) {
        try {
            PlanInventaire plan = planInventaireRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Plan non trouvé avec l'id: " + planId));
            
            Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new EntityNotFoundException("Produit non trouvé avec l'id: " + produitId));

            plan.getProduits().remove(produit);
            planInventaireRepository.save(plan);

            if (plan.getProduits().isEmpty()) {
                plan.setStatut(STATUS.Termine);
                planInventaireRepository.save(plan);
            }

            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Mettre à jour le statut du plan")
    @ApiResponse(responseCode = "200", description = "Statut mis à jour avec succès")
    @PutMapping("/{planId}/statut")
    public ResponseEntity<?> updatePlanStatut(
            @PathVariable Long planId,
            @RequestBody Map<String, String> statutRequest) {
        try {
            PlanInventaire plan = planInventaireRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Plan non trouvé avec l'id: " + planId));

            String nouveauStatut = statutRequest.get("statut");
            if (nouveauStatut == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le statut est requis"));
            }

            try {
                STATUS status = STATUS.valueOf(nouveauStatut);
                plan.setStatut(status);
                planInventaireRepository.save(plan);
                return ResponseEntity.ok().build();
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Statut invalide"));
            }
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Récupérer les produits d'un plan")
    @ApiResponse(responseCode = "200", description = "Liste des produits récupérée avec succès")
    @GetMapping("/{planId}/produits")
    public ResponseEntity<List<Produit>> getPlanProduits(@PathVariable Long planId) {
        try {
            PlanInventaire plan = planInventaireRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Plan non trouvé avec l'id: " + planId));
            
            return ResponseEntity.ok(new ArrayList<>(plan.getProduits()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException e) {
        String message;
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            message = e.getMessage();
        } else {
            message = "[" + e.getClass().getSimpleName() + "] Une erreur interne est survenue";
        }
        return ResponseEntity
            .badRequest()
            .body(java.util.Collections.singletonMap("error", message));
    }

    @GetMapping("/{planId}/details")
    @Transactional
    public ResponseEntity<?> getPlanDetails(@PathVariable Long planId) {
        return planInventaireRepository.findById(planId)
                .map(plan -> ResponseEntity.ok(convertToDTO(plan)))
                .orElse(ResponseEntity.notFound().build());
    }
    @PatchMapping("{planId}")
    public ResponseEntity<?> patchPlan(@PathVariable Long planId, @RequestBody Map<String, Object> updates) {
        Optional<PlanInventaire> optionalPlan = planInventaireRepository.findById(planId);
        if (optionalPlan.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        PlanInventaire plan = optionalPlan.get();
        if (updates.containsKey("nom")) {
            plan.setNom((String) updates.get("nom"));
        }
        if (updates.containsKey("dateDebut")) {
            plan.setDateDebut(LocalDateTime.parse((String) updates.get("dateDebut")));
        }
        if (updates.containsKey("dateFin")) {
            plan.setDateFin(LocalDateTime.parse((String) updates.get("dateFin")));
        }
        if (updates.containsKey("type")) {
            plan.setType(TYPE.valueOf((String) updates.get("type")));
        }
        planInventaireRepository.save(plan);
        return ResponseEntity.ok().build();
    }

private PlanInventaireDTO convertToDTO(PlanInventaire plan) {
    try {
        PlanInventaireDTO dto = new PlanInventaireDTO();
        dto.setId(plan.getId());
        dto.setNom(plan.getNom());
        dto.setDateDebut(plan.getDateDebut());
        dto.setDateFin(plan.getDateFin());
        dto.setType(plan.getType() != null ? plan.getType().toString() : null);
        dto.setRecurrence(plan.getRecurrence() != null ? plan.getRecurrence().toString() : null);
        dto.setStatut(plan.getStatut() != null ? plan.getStatut().toString() : null);
        dto.setInclusTousProduits(plan.isInclusTousProduits());
        dto.setDateCreation(plan.getDateCreation());
        List<Zone> zonesList = new ArrayList<>(plan.getZones());
        Map<Long, PlanInventaireDTO.ZoneDTO> zoneMap = new HashMap<>();
        
        for (Zone zone : zonesList) {
            try {
                PlanInventaireDTO.ZoneDTO z = new PlanInventaireDTO.ZoneDTO();
                z.setId(zone.getId());
                z.setName(zone.getName());
                dto.getZones().add(z);
                zoneMap.put(z.getId(), z);
            } catch (Exception e) {
                log.error("Erreur lors de la conversion de la zone {}: {}", zone.getId(), e.getMessage());
            }
        }
        List<AssignationAgent> assignations = assignationAgentRepository.findByPlanInventaireId(plan.getId());


        for (AssignationAgent ass : assignations) {
            try {

                PlanInventaireDTO.AgentInventaireDTO agentDTO = new PlanInventaireDTO.AgentInventaireDTO();
                agentDTO.setId(ass.getAgent().getId());
                agentDTO.setFirstName(ass.getAgent().getPrenom());
                agentDTO.setLastName(ass.getAgent().getNom());
                agentDTO.setEmail(ass.getAgent().getEmail());
                agentDTO.setRole(String.valueOf(ass.getAgent().getRole()));

                PlanInventaireDTO.ZoneDTO zoneDTO = zoneMap.get(ass.getZone().getId());

                PlanInventaireDTO.AssignationAgentDTO assignationDTO = new PlanInventaireDTO.AssignationAgentDTO();
                assignationDTO.setId(ass.getId());
                assignationDTO.setDateAssignation(ass.getDateAssignation());
                assignationDTO.setAgent(agentDTO);
                assignationDTO.setZone(zoneDTO);
                dto.getAssignations().add(assignationDTO);

            } catch (Exception e) {
                log.error("Erreur lors de la conversion de l'assignation ");
            }
        }
        if (plan.getCreateur() != null) {
            try {
                PlanInventaireDTO.UtilisateurDTO createurDTO = new PlanInventaireDTO.UtilisateurDTO();
                createurDTO.setId(plan.getCreateur().getId());
                createurDTO.setNom(plan.getCreateur().getNom());
                createurDTO.setPrenom(plan.getCreateur().getPrenom());
                createurDTO.setEmail(plan.getCreateur().getEmail());
                createurDTO.setRole(plan.getCreateur().getRole() != null ? plan.getCreateur().getRole().toString() : null);
                createurDTO.setDateCreation(plan.getCreateur().getDateCreation());
                createurDTO.setDateModification(plan.getCreateur().getDatecremod());
                dto.setCreateur(createurDTO);
            } catch (Exception e) {
                log.error("Erreur lors de la conversion du créateur");
            }
        }
        return dto;
    } catch (Exception e) {
        log.error("Erreur lors de la conversion du plan");
        throw new RuntimeException("Erreur lors de la conversion du plan: " + e.getMessage());
    }
}
private PlanInventaireDTO.ProduitDTO convertProduitToDTO(Produit produit) {
    PlanInventaireDTO.ProduitDTO produitDTO = new PlanInventaireDTO.ProduitDTO();
    produitDTO.setId(produit.getId());
    if (produit.getCategory() != null) {
        PlanInventaireDTO.CategoryDTO categoryDTO = new PlanInventaireDTO.CategoryDTO();
        categoryDTO.setId(produit.getCategory().getId());
        categoryDTO.setName(produit.getCategory().getName());
        produitDTO.setCategoryDTO(categoryDTO);
    }
    if (produit.getSubCategory() != null) {
        PlanInventaireDTO.SubCategoryDTO subCategoryDTO = new PlanInventaireDTO.SubCategoryDTO();
        subCategoryDTO.setId(produit.getSubCategory().getId());
        subCategoryDTO.setName(produit.getSubCategory().getName());
        produitDTO.setSubCategoryDTO(subCategoryDTO);
    }
    return produitDTO;
}
    @GetMapping("/{planId}/zone-products")
    public ResponseEntity<Map<Long, List<Map<String, Object>>>> getZoneProducts(@PathVariable Long planId) {
        try {
            PlanInventaire plan = planInventaireRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Plan not found with id: " + planId));
            Map<Long, List<Map<String, Object>>> zoneProducts = new HashMap<>();

            Set<Produit> planProduits = plan.getProduits();
            for (Zone zone : plan.getZones()) {
                List<Map<String, Object>> produits = new ArrayList<>();
                for (Produit produit : zone.getProduits()) {
                    if (!planProduits.contains(produit)) continue;
                    ZoneProduit zoneProduit = produit.getZoneProduits().stream()
                        .filter(zp -> zp.getZone().getId().equals(zone.getId()) && zp.getProduit().getId().equals(produit.getId()))
                        .findFirst().orElse(null);
                    Map<String, Object> produitMap = new HashMap<>();
                    produitMap.put("id", produit.getId());
                    produitMap.put("nom", produit.getNom());
                    produitMap.put("codeBarre", produit.getCodeBarre());
                    produitMap.put("quantiteTheorique", zoneProduit != null ? zoneProduit.getQuantiteTheorique() : 0);
                    produits.add(produitMap);
                }
                zoneProducts.put(zone.getId(), produits);
            }
            return ResponseEntity.ok(zoneProducts);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private LocalDateTime parseDateTime(String input) {
        if (input == null) {
            throw new IllegalArgumentException("date null");
        }
        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        };
        for (DateTimeFormatter f : formatters) {
            try {
                return LocalDateTime.parse(input, f);
            } catch (DateTimeParseException ignored) { }
        }
        throw new IllegalArgumentException("Format de date invalide: " + input);
    }
    @GetMapping("/{planId}/validated-products")
    public ResponseEntity<List<Map<String, Object>>> getValidatedProducts(@PathVariable Long planId) {
        PlanInventaire plan = planInventaireRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Plan not found with id: " + planId));

        List<ZoneProduitPlanValide> validatedList = zoneProduitPlanValideRepository.findByPlan(plan);
        List<Map<String, Object>> validated = new ArrayList<>();
        for (ZoneProduitPlanValide zpValide : validatedList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", zpValide.getProduit().getId());
            map.put("nom", zpValide.getProduit().getNom());
            map.put("codeBarre", zpValide.getProduit().getCodeBarre());
            map.put("validatedZone", zpValide.getZone().getId());
            map.put("quantiteValidee", zpValide.getQuantiteValidee());
            map.put("oldQuantiteAvant", zpValide.getOldQuantiteAvant());
            map.put("dateValidation", zpValide.getDateValidation());
            map.put("validatedByUserId", zpValide.getValidatedByUserId());
            validated.add(map);
        }
        return ResponseEntity.ok(validated);
    }
}
