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
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@RestController
@CrossOrigin("*")
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

    @Operation(
            summary = "Ajouter un nouveau checkup",
            description = "Crée un nouveau checkup (manuel ou scan) à partir des données fournies dans le corps de la requête.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Données du checkup à créer",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CheckupDTO.class),
                            examples = @ExampleObject(
                                    name = "Exemple de checkup manuel",
                                    value = "{\n  \"planId\": 1,\n  \"agentId\": 2,\n  \"type\": \"MANUEL\",\n  \"details\": [ { \"produitId\": 10, \"manualQuantity\": 5 } ]\n}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Checkup créé avec succès",
                    content = @Content(schema = @Schema(implementation = CheckupDTO.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PostMapping("/ajouter")
    public ResponseEntity<?> addCheckup(@RequestBody CheckupDTO checkupDTO) {
        try {
            Checkup checkup = convertToEntity(checkupDTO);
            Checkup savedCheckup = checkupRepository.save(checkup);
            CheckupDTO responseDTO = convertToDTO(savedCheckup);
        
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating checkup: " + e.getMessage());
        }
    }
    @Operation(
            summary = "Obtenir les checkups par plan et type",
            description = "Retourne les checkups associés à un plan donné, filtrés par type (`SCAN`, `MANUEL`, etc.)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des checkups récupérée avec succès",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Checkup.class)))),
            @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/plan/{planId}/type/{type}")
    public ResponseEntity<List<Checkup>> getCheckupsByPlanAndType(
            @PathVariable Long planId,
            @PathVariable CheckupType type) {
        try {
            List<Checkup> checkups = checkupService.findByPlanAndType(planId, type);
            return ResponseEntity.ok(checkups);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Scanner un produit",
            description = "Permet à un agent de scanner un produit (via son code-barres) et de l'ajouter au checkup d'un plan donné."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produit scanné et ajouté avec succès"),
            @ApiResponse(responseCode = "500", description = "Erreur interne du serveur", content = @Content)
    })
    @PostMapping("/scan")
    public ResponseEntity<String> scanProduit(
            @RequestParam Long agentId,
            @RequestParam Long planId,
            @RequestParam String CodeBarre) {
        try {
            checkupService.ajouterProduitParScan(agentId, planId, CodeBarre);
            return ResponseEntity.ok("Produit scanné et ajouté au checkup");
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
            @ApiResponse(responseCode = "200", description = "Recomptage demandé avec succès"),
            @ApiResponse(responseCode = "400", description = "Justification absente ou checkup introuvable",
                    content = @Content(mediaType = "text/plain"))
    })
    @PutMapping("/{checkupId}/recomptage")
    public ResponseEntity<?> demanderRecomptage(
            @PathVariable Long checkupId,
            @RequestBody RecomptageRequest request
    ) {
        try {
            if (request.getJustification() == null || request.getJustification().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("La justification est requise");
            }
            Checkup checkup = checkupRepository.findById(checkupId)
                    .orElseThrow(() -> new RuntimeException("Checkup not found"));
            checkup.setDemandeRecomptage(Boolean.TRUE.equals(request.getDemandeRecomptage()));
            checkup.setJustificationRecomptage(request.getJustification());
            if (checkup.getDetails() != null) {
                checkup.getDetails().forEach(detail -> {
                    detail.setScannedQuantity(0);
                    detail.setManualQuantity(0);
                });
            }
            checkupRepository.save(checkup);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CheckupDTO.class)))),
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

        // Convert zones (simplified)
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
        checkup.setType(dto.getType());
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
                detail.setCheckup(checkup);

                if (detailDTO.getProduit() != null && detailDTO.getProduit().getId() != null) {
                    Produit produit = produitRepository.findById(detailDTO.getProduit().getId())
                            .orElseThrow(() -> new EntityNotFoundException("Produit not found with ID: " + detailDTO.getProduit().getId()));
                    detail.setProduit(produit);
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
        dto.setType(entity.getType());
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

            detailDTO.setProduit(produitDTO);
        }
        return detailDTO;
    }
}