package com.example.backend.controller;

import com.example.backend.dto.PlanInventaireDTO;
import com.example.backend.model.*;
import com.example.backend.repository.*;
import com.example.backend.service.PlanInventaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.responses.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.context.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.AllArgsConstructor;
import lombok.Getter;
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

    @Operation(summary = "Lister tous les plan")
    @ApiResponses(value =
    @ApiResponse(responseCode = "200", description = "lister tous les plans"))
    @GetMapping
    public ResponseEntity<List<PlanInventaire>> getAllPlans() {
        return ResponseEntity.ok(planInventaireRepository.findAll());
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
                // Ensure zones are loaded
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
            plan.setDateDebut(LocalDateTime.parse((String) requestData.get("dateDebut")));
            plan.setDateFin(LocalDateTime.parse((String) requestData.get("dateFin")));
            plan.setType(TYPE.valueOf((String) requestData.get("type")));
            String recurrenceStr = (String) requestData.get("recurrence");
            plan.setRecurrence(recurrenceStr != null ? RECCURENCE.valueOf(recurrenceStr) : RECCURENCE.MENSUEL);

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
        planInventaireRepository.deleteById(planId);
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
    @Operation(summary = "Enregistrer un nouvel utilisateur", description = "Crée un nouveau compte utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur enregistré avec succès"),
            @ApiResponse(responseCode = "400", description = "Données d'utilisateur invalides")
    })
    @PostMapping("{planId}/agents/{agentId}/assignations")
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
    @GetMapping("{planId}/assignations")
    public ResponseEntity<List<AssignationAgent>> getPlanAssignations(@PathVariable Long planId) {
        return ResponseEntity.ok(assignationAgentRepository.findByPlanInventaireId(planId));
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

            // Add product to plan if not already present
            if (!plan.getProduits().contains(produit)) {
                plan.getProduits().add(produit);
            }
        }

        // Batch save operations
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

            // Si c'était le dernier produit, vérifier si on doit mettre à jour le statut
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
            if (zone != null) {
                PlanInventaireDTO.ZoneDTO zoneDTO = new PlanInventaireDTO.ZoneDTO();
                zoneDTO.setId(zone.getId());

                // Map ZoneProduits with null check
                if (zone.getZoneProduits() != null) {
                    zone.getZoneProduits().forEach(zoneProduit -> {
                        if (zoneProduit != null) {
                            PlanInventaireDTO.ZoneProduitDTO zpDTO = new PlanInventaireDTO.ZoneProduitDTO();
                            zpDTO.setId(zoneProduit.getId().getProduitId());
                            zpDTO.setQuantitetheo(zoneProduit.getQuantiteTheorique());

                            // Map Produit in ZoneProduit
                            if (zoneProduit.getProduit() != null) {
                                zpDTO.setProduit(convertProduitToDTO(zoneProduit.getProduit()));
                            }

                            zoneDTO.getZoneProduits().add(zpDTO);
                        }
                    });
                }
                dto.getZones().add(zoneDTO);
            }
        });
    }

    // Map Produits with null check
    if (plan.getProduits() != null) {
        plan.getProduits().forEach(produit -> {
            if (produit != null) {
                dto.getProduits().add(convertProduitToDTO(produit));
            }
        });
    }
    if (plan.getAssignations() != null) {
        plan.getAssignations().forEach(assignation -> {
            if (assignation != null) {
                PlanInventaireDTO.AssignationAgentDTO assignationDTO = new PlanInventaireDTO.AssignationAgentDTO();
                assignationDTO.setId(assignation.getId());
                assignationDTO.setDateAssignation(assignation.getDateAssignation());

                if (assignation.getZone() != null) {
                    PlanInventaireDTO.ZoneDTO zoneDTO = new PlanInventaireDTO.ZoneDTO();
                    zoneDTO.setId(assignation.getZone().getId());
                    assignationDTO.setZone(zoneDTO);
                }

                if (assignation.getAgent() != null) {
                    PlanInventaireDTO.AgentDTO agentDTO = new PlanInventaireDTO.AgentDTO();
                    agentDTO.setId(assignation.getAgent().getId());
                    agentDTO.setNom(assignation.getAgent().getNom());
                    agentDTO.setPrenom(assignation.getAgent().getPrenom());
                    agentDTO.setEmail(assignation.getAgent().getEmail());
                    agentDTO.setRole(String.valueOf(assignation.getAgent().getRole()));
                    assignationDTO.setAgent(agentDTO);
                }

                dto.getAssignations().add(assignationDTO);
            }
        });
    }

    if (plan.getCreateur() != null) {
        PlanInventaireDTO.UtilisateurDTO createurDTO = new PlanInventaireDTO.UtilisateurDTO();
        createurDTO.setId(plan.getCreateur().getId());
        createurDTO.setNom(plan.getCreateur().getNom());
        createurDTO.setPrenom(plan.getCreateur().getPrenom());
        createurDTO.setEmail(plan.getCreateur().getEmail());
        createurDTO.setRole(String.valueOf(plan.getCreateur().getRole()));
        createurDTO.setDateCreation(plan.getCreateur().getDateCreation());
        dto.setCreateur(createurDTO);
    }

    return dto;
}

private PlanInventaireDTO.ProduitDTO convertProduitToDTO(Produit produit) {
    PlanInventaireDTO.ProduitDTO produitDTO = new PlanInventaireDTO.ProduitDTO();
    produitDTO.setId(produit.getId());

    // Map Category
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
            
            // Pour chaque zone du plan
            for (Zone zone : plan.getZones()) {
                List<Map<String, Object>> produits = new ArrayList<>();
                
                // Récupérer les produits qui sont à la fois dans la zone ET dans le plan
                List<Produit> zoneProduits = zone.getProduits();
                Set<Produit> planProduits = plan.getProduits();
                
                // Pour type PARTIEL, ne prendre que les produits qui sont dans le plan
                List<Produit> produitsToInclude = zoneProduits.stream()
                    .filter(planProduits::contains)
                    .collect(Collectors.toList());
                
                for (Produit produit : produitsToInclude) {
                    Map<String, Object> produitMap = new HashMap<>();
                    produitMap.put("id", produit.getId());
                    produitMap.put("nom", produit.getNom());
                    produitMap.put("codeBarre", produit.getCodeBarre());
                    // Ne pas inclure la quantité si elle n'est pas disponible dans le modèle
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

    @Getter
    @AllArgsConstructor
    private static class ErrorResponse {
        private final String error;
    }

    @GetMapping("/createdby/{userId}")
    public ResponseEntity<List<PlanInventaire>> getPlansByCreateur(@PathVariable Long userId) {
        List<PlanInventaire> plans = planInventaireRepository.findByCreateurId(userId);
        return ResponseEntity.ok(plans);
    }


}
