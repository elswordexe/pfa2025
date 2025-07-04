package com.example.backend.controller;

import com.example.backend.dto.CheckupDTO;
import com.example.backend.dto.PlanInventaireDTO;
import com.example.backend.model.*;
import com.example.backend.repository.*;
import com.example.backend.service.CheckupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@RestController
@CrossOrigin("*")
@Tag(name = "Checkups", description = "API pour la gestion des clients")
@RequestMapping("/checkups")
public class CheckupController {
    @Autowired
    private AgentInventaireRepository agentInventaireRepository;
    @Autowired
    CheckupService checkupService;
    @Autowired
    private CheckupRepository checkupRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PlanInventaireRepository planInventaireRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Operation(
        summary = "Ajouter un nouveau checkup",
        description = "Crée un nouveau checkup (manuel ou scan ou les deux) à partir des données fournies dans le corps de la requête.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Données du checkup à créer",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CheckupDTO.class),
                examples = {
                    @ExampleObject(
                        name = "Exemple de checkup manuel",
                        value = "{\n  \"agent\": { \"id\": 2 },\n  \"plan\": { \"id\": 1 },\n  \"details\": [ { \"produit\": { \"id\": 10 }, \"zone\": { \"id\": 3 }, \"manualQuantity\": 5 } ]\n}"
                    ),
                    @ExampleObject(
                        name = "Exemple de checkup scan",
                        value = "{\n  \"agent\": { \"id\": 2 },\n  \"plan\": { \"id\": 1 },\n  \"dateCheck\": \"2025-07-01T10:00:00\",\n  \"details\": [ { \"produit\": { \"id\": 11 }, \"zone\": { \"id\": 3 }, \"scannedQuantity\": 8 } ]\n}"
                    )
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Checkup créé avec succès",
            content = @Content(schema = @Schema(implementation = CheckupDTO.class),
                examples = @ExampleObject(
                    name = "Réponse checkup créé",
                    value = "{\n  \"id\": 123,\n  \"dateCheck\": \"2025-07-01T10:00:00\",\n  \"valide\": false,\n  \"demandeRecomptage\": false,\n  \"justificationRecomptage\": null,\n  \"agent\": { \"id\": 2, \"nom\": \"Dupont\", \"prenom\": \"Jean\", \"email\": \"jean.dupont@example.com\" },\n  \"plan\": { \"id\": 1, \"nom\": \"Inventaire Juillet\", \"dateDebut\": \"2025-07-01T00:00:00\", \"dateFin\": \"2025-07-02T00:00:00\" },\n  \"details\": [ { \"id\": 1, \"type\": \"MANUEL\", \"manualQuantity\": 5, \"scannedQuantity\": null, \"produit\": { \"id\": 10, \"codeBarre\": \"ABC123\", \"reference\": \"REF-10\", \"nom\": \"Produit A\", \"description\": \"Description produit A\", \"quantitetheo\": 100, \"category\": { \"id\": 1, \"name\": \"Catégorie 1\" }, \"subCategory\": { \"id\": 2, \"name\": \"Sous-catégorie 1\" } }, \"zone\": { \"id\": 3, \"name\": \"Zone 1\" } } ]\n}"
                )
            )
        ),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PostMapping("/ajouter")
    public ResponseEntity<?> addCheckup(@RequestBody CheckupDTO checkupDTO) {
        try {
            if (checkupDTO.getDetails() != null && !checkupDTO.getDetails().isEmpty()) {
                for (CheckupDTO.CheckupDetailDTO dtoDetail : checkupDTO.getDetails()) {
                    List<Checkup> existingCheckups = checkupRepository.findByPlanId(checkupDTO.getPlan().getId());
                    Checkup existing = null;
                    CheckupDetail existingDetail = null;
                    for (Checkup c : existingCheckups) {
                        if (c.getAgent() != null && c.getAgent().getId().equals(checkupDTO.getAgent().getId())) {
                            for (CheckupDetail d : c.getDetails()) {
                                if (d.getProduit().getId().equals(dtoDetail.getProduit().getId()) &&
                                    d.getZone().getId().equals(dtoDetail.getZone().getId()) &&
                                    d.getType() == dtoDetail.getType()) {
                                    existing = c;
                                    existingDetail = d;
                                    break;
                                }
                            }
                        }
                        if (existing != null) break;
                    }
                    if (existing != null && existingDetail != null) {
  
                        if (dtoDetail.getManualQuantity() != null)
                            existingDetail.setManualQuantity(dtoDetail.getManualQuantity());
                        if (dtoDetail.getScannedQuantity() != null)
                            existingDetail.setScannedQuantity(dtoDetail.getScannedQuantity());
                        checkupRepository.save(existing);
                        CheckupDTO responseDTO = convertToDTO(existing);
                        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
                    }
                }
            }
            Checkup checkup = convertToEntity(checkupDTO);
            if (checkupDTO.getDetails() != null && checkup.getDetails() != null) {
                for (int i = 0; i < checkupDTO.getDetails().size(); i++) {
                    CheckupDTO.CheckupDetailDTO dtoDetail = checkupDTO.getDetails().get(i);
                    CheckupDetail entityDetail = checkup.getDetails().get(i);
                    entityDetail.setManualQuantity(dtoDetail.getManualQuantity());
                    entityDetail.setScannedQuantity(dtoDetail.getScannedQuantity());
                }
            }
            Checkup savedCheckup = checkupRepository.save(checkup);
            CheckupDTO responseDTO = convertToDTO(savedCheckup);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating checkup: " + e.getMessage());
        }
    }
    @Operation(
            summary = "Obtenir les checkups par plan et type",
            description = "Retourne les checkups associés à un plan donné"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des checkups récupérée avec succès",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Checkup.class)))),
            @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/plan/{planId}/type/ALL")
    public ResponseEntity<List<CheckupDTO>> getCheckupsByPlan(
            @PathVariable Long planId) {
        try {
            List<Checkup> checkups = checkupRepository.findByPlanId(planId);
            List<CheckupDTO> dtos = checkups.stream().map(this::convertToDTO).toList();
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Scanner un produit",
        description = "Permet à un agent de scanner un produit (via son code-barres) et de l'ajouter au checkup d'un plan donné.",
        parameters = {
            @io.swagger.v3.oas.annotations.Parameter(
                name = "agentId",
                description = "ID de l'agent qui scanne le produit",
                required = true,
                example = "2",
                schema = @Schema(type = "integer", format = "int64")
            ),
            @io.swagger.v3.oas.annotations.Parameter(
                name = "planId",
                description = "ID du plan d'inventaire concerné",
                required = true,
                example = "1",
                schema = @Schema(type = "integer", format = "int64")
            ),
            @io.swagger.v3.oas.annotations.Parameter(
                name = "barrecode",
                description = "Code-barres du produit à scanner (dans l'URL)",
                required = true,
                example = "ABC123",
                schema = @Schema(type = "string")
            )
        }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Produit scanné et ajouté avec succès",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(type = "string", example = "Produit scanné et ajouté au checkup")
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erreur interne du serveur",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(type = "string", example = "Erreur : message d'erreur technique")
            )
        )
    })
    @PostMapping("/scan/{barrecode}")
    public ResponseEntity<String> scanProduit(
            @RequestParam Long agentId,
            @RequestParam Long planId,
            @PathVariable("barrecode") String barrecode) {
        try {
            checkupService.ajouterProduitParScan(agentId, planId, barrecode);
            return ResponseEntity.status(HttpStatus.CREATED).body("Produit scanné et ajouté au checkup");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur : " + e.getMessage());
        }
    }

    @Operation(
            summary = "Valider un checkup",
            description = "Valide un checkup spécifique. Si les quantités scannées correspondent aux quantités manuelles, les quantités théoriques des produits sont mises à jour. Si tous les produits sont traités, le plan passe en statut 'Terminé'."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Checkup validé avec succès"),
            @ApiResponse(responseCode = "400", description = "Checkup introuvable ou erreur de validation")
    })
    @PutMapping("/{checkupId}/valider")
    public ResponseEntity<?> validerCheckup(@PathVariable Long checkupId) {
        try {
            Checkup checkup = checkupRepository.findById(checkupId)
                .orElseThrow(() -> new RuntimeException("Checkup not found"));

            for (CheckupDetail detail : checkup.getDetails()) {
                if (detail.getManualQuantity().equals(detail.getScannedQuantity())) {

                    Produit produit = detail.getProduit();
                    produit.setQuantitetheo(detail.getManualQuantity());
                    produitRepository.save(produit);

                    checkup.getPlan().getProduits().remove(produit);
                }
            }
            
            checkup.setValide(true);
            checkupRepository.save(checkup);
            PlanInventaire plan = checkup.getPlan();
            if (plan.getProduits().isEmpty()) {
                plan.setStatut(STATUS.valueOf("Termine"));
                planInventaireRepository.save(plan);
            }else{
                plan.setStatut(STATUS.valueOf("EN_cours"));
                planInventaireRepository.save(plan);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @Operation(
            summary = "Demander un recomptage",
            description = "Permet à un agent de demander un recomptage pour un checkup donné, en fournissant une justification obligatoire. Les quantités scannées et manuelles sont réinitialisées.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                description = "Justification du recomptage",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Exemple de demande de recomptage",
                        value = "{\n  \"justification\": \"Erreur de comptage initial\",\n  \"demandeRecomptage\": true\n}"
                    )
                )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recomptage demandé avec succès",
                    content = @Content(
                        examples = @ExampleObject(
                            name = "Réponse recomptage",
                            value = "{\n  \"message\": \"Recomptage demandé avec succès pour le produit dans la zone spécifiée\",\n  \"justification\": \"Erreur de comptage initial\",\n  \"checkupId\": 123,\n  \"produitId\": 10,\n  \"zoneId\": 3\n}"
                        )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Justification absente ou checkup introuvable",
                    content = @Content(mediaType = "text/plain"))
    })
    @PutMapping("/{checkupId}/recomptage")
    public ResponseEntity<?> demanderRecomptage(
            @PathVariable Long checkupId,
            @RequestParam Long produitId,
            @RequestParam Long zoneId,
            @RequestBody RecomptageRequest request
    ) {
        try {
            if (request.getJustification() == null || request.getJustification().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("La justification est requise");
            }

            Checkup checkup = checkupRepository.findById(checkupId)
                    .orElseThrow(() -> new RuntimeException("Checkup not found"));

            checkup.setDemandeRecomptage(true);
            checkup.setJustificationRecomptage(request.getJustification());

            if (checkup.getDetails() != null) {
                for (CheckupDetail detail : checkup.getDetails()) {
                    if (detail.getProduit().getId().equals(produitId) && 
                        detail.getZone().getId().equals(zoneId)) {
                        detail.setManualQuantity(null);
                        detail.setScannedQuantity(null);
                        break;
                    }
                }
            }
            checkupRepository.save(checkup);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Recomptage demandé avec succès pour le produit dans la zone spécifiée");
            response.put("justification", checkup.getJustificationRecomptage());
            response.put("checkupId", checkup.getId());
            response.put("produitId", produitId);
            response.put("zoneId", zoneId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{checkupId}/reset-quantities")
    public ResponseEntity<?> resetQuantities(
            @PathVariable Long checkupId,
            @RequestParam Long produitId,
            @RequestParam Long zoneId
    ) {
        try {
            Checkup checkup = checkupRepository.findById(checkupId)
                    .orElseThrow(() -> new RuntimeException("Checkup not found"));

            boolean quantitiesReset = false;
            if (checkup.getDetails() != null) {
                for (CheckupDetail detail : checkup.getDetails()) {
                    if (detail.getProduit().getId().equals(produitId) && 
                        detail.getZone().getId().equals(zoneId)) {
                        detail.setManualQuantity(null);
                        detail.setScannedQuantity(null);
                        quantitiesReset = true;
                        break;
                    }
                }
            }

            if (!quantitiesReset) {
                return ResponseEntity.badRequest()
                    .body("Aucun détail trouvé pour ce produit dans cette zone");
            }

            checkupRepository.save(checkup);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Quantités réinitialisées avec succès");
            response.put("checkupId", checkupId);
            response.put("produitId", produitId);
            response.put("zoneId", zoneId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("plan/{id}")
    public ResponseEntity<PlanInventaireDTO> getPlanById(@PathVariable Long id) {
        return planInventaireRepository.findById(id)
        .map(this::convertToDTO)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
    }



    @Operation(
            summary = "Obtenir tous les checkups d'un plan",
            description = "Retourne tous les checkups (SCAN et MANUEL) associés à un plan spécifique, avec leurs informations principales."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des checkups retournée avec succès",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CheckupDTO.class)),
                        examples = @ExampleObject(
                            name = "Réponse logs",
                            value = "[{\n  \"id\": 123,\n  \"dateCheck\": \"2025-07-01T10:00:00\",\n  \"valide\": false,\n  \"demandeRecomptage\": false,\n  \"justificationRecomptage\": null,\n  \"agent\": { \"id\": 2, \"nom\": \"Dupont\", \"prenom\": \"Jean\", \"email\": \"jean.dupont@example.com\" },\n  \"plan\": { \"id\": 1, \"nom\": \"Inventaire Juillet\", \"dateDebut\": \"2025-07-01T00:00:00\", \"dateFin\": \"2025-07-02T00:00:00\" },\n  \"details\": [ { \"id\": 1, \"type\": \"MANUEL\", \"manualQuantity\": 5, \"scannedQuantity\": null, \"produit\": { \"id\": 10, \"codeBarre\": \"ABC123\", \"reference\": \"REF-10\", \"nom\": \"Produit A\", \"description\": \"Description produit A\", \"quantitetheo\": 100, \"category\": { \"id\": 1, \"name\": \"Catégorie 1\" }, \"subCategory\": { \"id\": 2, \"name\": \"Sous-catégorie 1\" } }, \"zone\": { \"id\": 3, \"name\": \"Zone 1\" } } ]\n}]"
                        )
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/plan/{planId}/logs")
    public ResponseEntity<List<CheckupDTO>> getLogsByPlan(@PathVariable Long planId) {
        try {
            List<Checkup> checkups = checkupRepository.findByPlanId(planId);
            List<CheckupDTO> logs = checkups.stream()
                    .map(this::convertToDTO)
                    .toList();
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/scan/{checkupId}")
    public ResponseEntity<?> updateScannedQuantity(
            @PathVariable Long checkupId,
            @RequestParam Long produitId,
            @RequestParam Long zoneId,
            @RequestParam(required = false) Integer quantity
    ) {
        try {
            Checkup checkup = checkupRepository.findById(checkupId)
                    .orElseThrow(() -> new RuntimeException("Checkup not found"));

            boolean detailFound = false;
            if (checkup.getDetails() != null) {
                for (CheckupDetail detail : checkup.getDetails()) {
                    if (detail.getProduit().getId().equals(produitId) && 
                        detail.getZone().getId().equals(zoneId)) {
                        Integer currentQuantity = detail.getScannedQuantity();
                        if (currentQuantity == null) {
                            currentQuantity = 0;
                        }
                        detail.setScannedQuantity(currentQuantity + (quantity != null ? quantity : 1));
                        detailFound = true;
                        break;
                    }
                }
            }

            if (!detailFound) {
                return ResponseEntity.badRequest()
                    .body("Aucun détail trouvé pour ce produit dans cette zone");
            }

            checkupRepository.save(checkup);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Quantité scannée mise à jour avec succès");
            response.put("checkupId", checkupId);
            response.put("produitId", produitId);
            response.put("zoneId", zoneId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    private PlanInventaireDTO convertToDTO(PlanInventaire plan) {
        PlanInventaireDTO dto = new PlanInventaireDTO();
        dto.setId(plan.getId());
        dto.setNom(plan.getNom());
        dto.setDateDebut(plan.getDateDebut());
        dto.setDateFin(plan.getDateFin());
        dto.setType(String.valueOf(plan.getType()));
        dto.setRecurrence(String.valueOf(plan.getRecurrence()));
        dto.setStatut(String.valueOf(plan.getStatut()));
        dto.setInclusTousProduits(plan.isInclusTousProduits());
        dto.setDateCreation(plan.getDateCreation());
        if (plan.getZones() != null) {
            plan.getZones().forEach(zone -> {
                PlanInventaireDTO.ZoneDTO zoneDTO = new PlanInventaireDTO.ZoneDTO();
                zoneDTO.setId(zone.getId());
                dto.getZones().add(zoneDTO);
            });
        }

        if (plan.getProduits() != null) {
            plan.getProduits().forEach(produit -> {
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
                dto.getProduits().add(produitDTO);
            });
        }
        if (plan.getCreateur() != null) {
            PlanInventaireDTO.UtilisateurDTO utilisateurDTO = new PlanInventaireDTO.UtilisateurDTO();
            utilisateurDTO.setId(plan.getCreateur().getId());
            utilisateurDTO.setNom(plan.getCreateur().getNom());
            utilisateurDTO.setPrenom(plan.getCreateur().getPrenom());
            utilisateurDTO.setEmail(plan.getCreateur().getEmail());
            utilisateurDTO.setRole(String.valueOf(plan.getCreateur().getRole()));
            dto.setCreateur(utilisateurDTO);
        }

        return dto;
    }  private Checkup convertToEntity(CheckupDTO dto) {
        Checkup checkup = new Checkup();
        checkup.setId(dto.getId());
        checkup.setValide(dto.isValide());
        checkup.setDemandeRecomptage(dto.isDemandeRecomptage());
        checkup.setJustificationRecomptage(dto.getJustificationRecomptage());
        checkup.setDateCheck(dto.getDateCheck() != null ? dto.getDateCheck() : LocalDateTime.now());
        if (dto.getAgent() != null && dto.getAgent().getId() != null) {
            AgentInventaire agent = agentInventaireRepository.findById(dto.getAgent().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Agent not found with ID: " + dto.getAgent().getId()));
            checkup.setAgent(agent);
        }
        if (dto.getPlan() != null && dto.getPlan().getId() != null) {
            PlanInventaire plan = planInventaireRepository.findById(dto.getPlan().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Plan not found with ID: " + dto.getPlan().getId()));
            checkup.setPlan(plan);
        }
        if (dto.getDetails() != null && !dto.getDetails().isEmpty()) {
            List<CheckupDetail> details = new ArrayList<>();
            for (CheckupDTO.CheckupDetailDTO detailDTO : dto.getDetails()) {
                CheckupDetail detail = new CheckupDetail();
                detail.setId(detailDTO.getId());
                detail.setScannedQuantity(detailDTO.getScannedQuantity());
                detail.setManualQuantity(detailDTO.getManualQuantity());
                detail.setType(detailDTO.getType());
                detail.setCheckup(checkup);

                if (detailDTO.getProduit() != null && detailDTO.getProduit().getId() != null) {
                    Produit produit = produitRepository.findById(detailDTO.getProduit().getId())
                            .orElseThrow(() -> new EntityNotFoundException("Produit not found with ID: " + detailDTO.getProduit().getId()));
                    detail.setProduit(produit);
                }
                if (detailDTO.getZone() != null && detailDTO.getZone().getId() != null) {
                    Zone zone = zoneRepository.findById(detailDTO.getZone().getId())
                            .orElseThrow(() -> new EntityNotFoundException("Zone not found with ID: " + detailDTO.getZone().getId()));
                    detail.setZone(zone);
                }
                details.add(detail);
            }
            checkup.setDetails(details);
        }

        return checkup;
    }

    private CheckupDTO convertToDTO(Checkup entity) {
        CheckupDTO dto = new CheckupDTO();

        dto.setId(entity.getId());
        dto.setDateCheck(entity.getDateCheck());
        dto.setValide(entity.isValide());
        dto.setDemandeRecomptage(entity.isDemandeRecomptage());
        dto.setJustificationRecomptage(entity.getJustificationRecomptage());

        if (entity.getAgent() != null) {
            CheckupDTO.AgentDTO agentDTO = new CheckupDTO.AgentDTO();
            agentDTO.setId(entity.getAgent().getId());
            agentDTO.setNom(entity.getAgent().getNom());
            agentDTO.setPrenom(entity.getAgent().getPrenom());
            agentDTO.setEmail(entity.getAgent().getEmail());
            dto.setAgent(agentDTO);
        }

        if (entity.getPlan() != null) {
            CheckupDTO.PlanDTO planDTO = new CheckupDTO.PlanDTO();
            planDTO.setId(entity.getPlan().getId());
            planDTO.setNom(entity.getPlan().getNom());
            planDTO.setDateDebut(entity.getPlan().getDateDebut());
            planDTO.setDateFin(entity.getPlan().getDateFin());
            dto.setPlan(planDTO);
        }

        if (entity.getDetails() != null && !entity.getDetails().isEmpty()) {
            List<CheckupDTO.CheckupDetailDTO> detailDTOs = new ArrayList<>();
            for (CheckupDetail detail : entity.getDetails()) {
                CheckupDTO.CheckupDetailDTO detailDTO = getCheckupDetailDTO(detail);
                detailDTOs.add(detailDTO);
            }
            dto.setDetails(detailDTOs);
        }
        return dto;
    }

    private static CheckupDTO.CheckupDetailDTO getCheckupDetailDTO(CheckupDetail detail) {
        CheckupDTO.CheckupDetailDTO detailDTO = new CheckupDTO.CheckupDetailDTO();
        detailDTO.setId(detail.getId());
        detailDTO.setScannedQuantity(detail.getScannedQuantity());
        detailDTO.setManualQuantity(detail.getManualQuantity());
        detailDTO.setType(detail.getType());
        if (detail.getProduit() != null) {
            CheckupDTO.ProduitDTO produitDTO = new CheckupDTO.ProduitDTO();
            produitDTO.setId(detail.getProduit().getId());
            produitDTO.setCodeBarre(detail.getProduit().getCodeBarre());
            produitDTO.setReference(detail.getProduit().getReference());
            produitDTO.setNom(detail.getProduit().getNom());
            produitDTO.setDescription(detail.getProduit().getDescription());
            produitDTO.setQuantitetheo(detail.getProduit().getQuantitetheo());
            if (detail.getProduit().getCategory() != null) {
                CheckupDTO.ProduitDTO.CategoryDTO categoryDTO = new CheckupDTO.ProduitDTO.CategoryDTO();
                categoryDTO.setId(detail.getProduit().getCategory().getId());
                categoryDTO.setName(detail.getProduit().getCategory().getName());
                produitDTO.setCategory(categoryDTO);
            }
            if (detail.getProduit().getSubCategory() != null) {
                CheckupDTO.ProduitDTO.SubCategoryDTO subCategoryDTO = new CheckupDTO.ProduitDTO.SubCategoryDTO();
                subCategoryDTO.setId(detail.getProduit().getSubCategory().getId());
                subCategoryDTO.setName(detail.getProduit().getSubCategory().getName());
                produitDTO.setSubCategory(subCategoryDTO);
            }
            if (detail.getZone() != null) {
                CheckupDTO.ZoneDTO z = new CheckupDTO.ZoneDTO();
                z.setId(detail.getZone().getId());
                z.setName(detail.getZone().getName());
                detailDTO.setZone(z);
            }
            detailDTO.setProduit(produitDTO);
        }
        return detailDTO;
    }
}