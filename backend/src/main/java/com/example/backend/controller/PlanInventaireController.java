package com.example.backend.controller;

import com.example.backend.dto.PlanInventaireDTO;
import com.example.backend.model.*;
import com.example.backend.repository.*;
import com.example.backend.service.PlanInventaireService;
import io.swagger.v3.oas.annotations.Operation;
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
import io.swagger.v3.oas.annotations.media.ExampleObject;

@RestController
@RequestMapping("/api/plans")
@CrossOrigin(origins = "*")
@Tag(name = "Plan d'Inventaire", description = "API de gestion des plans d'inventaire")
@Slf4j
@RequiredArgsConstructor
public class PlanInventaireController {

    private final PlanInventaireService planInventaireService;
    @Autowired
    private final PlanInventaireRepository planInventaireRepository;
    @Autowired
    private final ProduitRepository produitRepository;
    @Autowired
    private final ZoneRepository zoneRepository;
    @Autowired
    private final UtilisateurRepository utilisateurRepository;
    @Autowired
    private final AssignationAgentRepository assignationAgentRepository;
    @Autowired
    private final EcartRepository ecartRepository;
    @Autowired
    private final CheckupRepository checkupRepository;

    @Operation(summary = "Lister tous les plan")
    @ApiResponses(value =
    @ApiResponse(responseCode = "200", description = "lister tous les plans"))
    @GetMapping
    @Transactional
    public ResponseEntity<List<PlanInventaireDTO>> getAllPlans() {
        List<PlanInventaire> plans = planInventaireRepository.findAll();
        plans.forEach(plan -> {
            plan.getZones().size();
            plan.getProduits().size();
            if (plan.getAssignations() != null) plan.getAssignations().size();
        });
        List<PlanInventaireDTO> dtos = plans.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "retourner un plan", description = "lister les infos du plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "lister les infos du plan avec succès"),
            @ApiResponse(responseCode = "404", description = "plan introuvables")
    })
    @GetMapping("{planId}")
    public ResponseEntity<PlanInventaire> getPlanById(@PathVariable Long planId) {
        return planInventaireRepository.findById(planId)
            .map(plan -> {
                plan.getProduits().forEach(produit -> {
                    if (produit.getZones() == null) {
                        produit.setZones(new ArrayList<>());
                    }
                });
                return ResponseEntity.ok(plan);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Créer un plan d'inventaire",
            description = "Créer un nouveau plan avec nom, dates, statut, zones, produits et type",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Corps de la requête pour créer un plan",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Exemple de création de plan",
                                    value = "{\n  \"nom\": \"Inventaire trimestriel\",\n  \"dateDebut\": \"2025-01-01T08:00:00\",\n  \"dateFin\": \"2025-01-10T18:00:00\",\n  \"type\": \"COMPLET\",\n  \"recurrence\": \"MENSUEL\",\n  \"statut\": \"Planifie\",\n  \"createur\": { \"id\": 1 },\n  \"zones\": [ { \"id\": 1 }, { \"id\": 2 } ]\n}"
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Plan d'inventaire créé",
                            content = @Content(schema = @Schema(implementation = Map.class)))
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
            
            return ResponseEntity.ok(response);

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

        // Supprimer d'abord les checkups liés à ce plan pour éviter les contraintes FK
        List<Checkup> planCheckups = checkupRepository.findByPlanId(planId);
        checkupRepository.deleteAll(planCheckups);

        // Supprimer les assignations d'agent liées
        List<AssignationAgent> assigns = assignationAgentRepository.findByPlanInventaireId(planId);
        assignationAgentRepository.deleteAll(assigns);

        // On peut maintenant supprimer le plan
        planInventaireRepository.delete(plan);

        return ResponseEntity.ok().build();
    }
    @Operation(summary = "mise a jour du plan", description = "update du plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "mise a jour du plan avec succès"),
            @ApiResponse(responseCode = "400", description = "errur")
    })
    @PutMapping("{planId}")
    public ResponseEntity<?> modifierPlan(@PathVariable Long planId, @RequestBody PlanInventaire planInventaire) {
        if (!planId.equals(planInventaire.getId())) {
            return ResponseEntity.badRequest().build();
        }
        PlanInventaire plan = planInventaireRepository.save(planInventaire);
        return ResponseEntity.ok(plan);
    }
    @Operation(summary = "assigner un agent ", description = "assigner un aplan a un agent")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "agent assigne  avec succès"),
            @ApiResponse(responseCode = "400", description = "failure d'assignations")
    })
    @PostMapping("{planId}/agents/{agentId}/assignations")
    public ResponseEntity<?> assignerAgents(@PathVariable Long planId,
                                            @PathVariable Long agentId,
                                            @RequestBody(required = false) Zone zone,
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

            return ResponseEntity.ok(assignationDTOs);
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
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Liste d'objets Zone à associer au plan",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Zone.class),
                            examples = @ExampleObject(
                                    name = "Exemple de zones",
                                    value = "[ { \"id\": 1 }, { \"id\": 2 } ]"
                            )
                    )
            ),responses= {
        @ApiResponse(responseCode = "200", description = "Zones ajoutées",
                content = @Content(schema = @Schema(implementation = PlanInventaire.class)))
    }
    )
    @PostMapping("{planId}/zones")
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
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Liste des produits avec les zones où ils se trouvent",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Exemple de produits & zones",
                                    value = "[ { \"produitId\": 10, \"zoneId\": 1 }, { \"produitId\": 11, \"zoneId\": 2 } ]"
                            )
                    )
            ),
                    responses = {
                            @ApiResponse(responseCode = "200", description = "Produits ajoutés",
                                    content = @Content(schema = @Schema(implementation = PlanInventaire.class)))
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

            for (Zone existingZone : new ArrayList<>(produit.getZones())) {
                existingZone.getProduits().remove(produit);
                produit.getZones().remove(existingZone);
            }

            for (Long zoneId : zoneIds) {
                Zone zone = zoneRepository.findById(zoneId)
                        .orElseThrow(() -> new EntityNotFoundException("Zone non trouvée: " + zoneId));
                produit.getZones().add(zone);
                zone.getProduits().add(produit);
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

        return ResponseEntity.ok()
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
        return ResponseEntity
            .badRequest()
            .body(Map.of("error", e.getMessage()));
    }

    @GetMapping("/{planId}/details")
    public ResponseEntity<?> getPlanDetails(@PathVariable Long planId) {
        return planInventaireRepository.findById(planId)
            .map(plan -> {
                PlanInventaireDTO dto = convertToDTO(plan);
                return ResponseEntity.ok(dto);
            })
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
        // Add more fields as needed
        planInventaireRepository.save(plan);
        return ResponseEntity.ok().build();
    }

private PlanInventaireDTO convertToDTO(PlanInventaire plan) {
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


    if (plan.getZones() != null) {
        plan.getZones().forEach(zone -> {
            PlanInventaireDTO.ZoneDTO zoneDTO = new PlanInventaireDTO.ZoneDTO();
            zoneDTO.setId(zone.getId());
            zoneDTO.setName(zone.getName());
            dto.getZones().add(zoneDTO);
        });
    }

    if (plan.getProduits() != null) {
        plan.getProduits().forEach(produit -> {
            dto.getProduits().add(convertProduitToDTO(produit));
        });
    }

    if (plan.getAssignations() != null) {
        plan.getAssignations().forEach(ass -> {
            PlanInventaireDTO.AssignationAgentDTO aaDTO = new PlanInventaireDTO.AssignationAgentDTO();
            aaDTO.setId(ass.getId());
            aaDTO.setDateAssignation(ass.getDateAssignation());

            if (ass.getZone() != null) {
                PlanInventaireDTO.ZoneDTO zoneDTO = new PlanInventaireDTO.ZoneDTO();
                zoneDTO.setId(ass.getZone().getId());
                zoneDTO.setName(ass.getZone().getName());
                aaDTO.setZone(zoneDTO);
            }

            if (ass.getAgent() != null) {
                PlanInventaireDTO.AgentInventaireDTO agentDTO = new PlanInventaireDTO.AgentInventaireDTO();
                agentDTO.setId(ass.getAgent().getId());
                agentDTO.setNom(ass.getAgent().getNom());
                agentDTO.setPrenom(ass.getAgent().getPrenom());
                agentDTO.setLastName(ass.getAgent().getNom());
                agentDTO.setFirstName(ass.getAgent().getPrenom());
                agentDTO.setEmail(ass.getAgent().getEmail());
                agentDTO.setRole(ass.getAgent().getRole() != null ? ass.getAgent().getRole().toString() : null);
                aaDTO.setAgent(agentDTO);
            }

            dto.getAssignations().add(aaDTO);
        });
    }

    if (plan.getCreateur() != null) {
        PlanInventaireDTO.UtilisateurDTO createurDTO = new PlanInventaireDTO.UtilisateurDTO();
        createurDTO.setId(plan.getCreateur().getId());
        createurDTO.setNom(plan.getCreateur().getNom());
        createurDTO.setPrenom(plan.getCreateur().getPrenom());
        createurDTO.setEmail(plan.getCreateur().getEmail());
        createurDTO.setRole(plan.getCreateur().getRole() != null ? plan.getCreateur().getRole().toString() : null);
        createurDTO.setDateCreation(plan.getCreateur().getDateCreation());
        createurDTO.setDateModification(plan.getCreateur().getDatecremod());
        dto.setCreateur(createurDTO);
    }
    return dto;
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

    // Map SubCategory
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

            for (Zone zone : plan.getZones()) {
                List<Produit> zoneProduits = zone.getProduits();
                // Exclude produits whose zoneProduit link is already verified
                zoneProduits = zoneProduits.stream()
                    .filter(prd -> {
                        return prd.getZoneProduits().stream()
                            .filter(zp -> zp.getZone().getId().equals(zone.getId()))
                            .noneMatch(ZoneProduit::isVerified);
                    })
                    .toList();
                
                List<Map<String, Object>> produits = new ArrayList<>();
                
                Set<Produit> planProduits = plan.getProduits();
                for (Produit produit : zoneProduits.stream().filter(planProduits::contains).toList()) {
                    Map<String, Object> produitMap = new HashMap<>();
                    produitMap.put("id", produit.getId());
                    produitMap.put("nom", produit.getNom());
                    produitMap.put("codeBarre", produit.getCodeBarre());
                    produitMap.put("quantitetheo", 0);
                    produits.add(produitMap);
                }
                
                zoneProducts.put(zone.getId(), produits);
            }
            
            return ResponseEntity.ok(zoneProducts);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/createdby/{userId}")
    public ResponseEntity<List<PlanInventaire>> getPlansByCreateur(@PathVariable Long userId) {
        List<PlanInventaire> plans = planInventaireRepository.findByCreateurId(userId);
        return ResponseEntity.ok(plans);
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

}
