package com.example.backend.controller;

import com.example.backend.dto.CheckupDTO;
import com.example.backend.dto.PlanInventaireDTO;
import com.example.backend.model.*;
import com.example.backend.repository.*;
import com.example.backend.service.CheckupService;
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

    @PostMapping("/ajouter")
    public ResponseEntity<?> addCheckup(@RequestBody CheckupDTO checkupDTO) {
        try {
            // Convert DTO to entity
            Checkup checkup = convertToEntity(checkupDTO);
        
            // Save the checkup
            Checkup savedCheckup = checkupRepository.save(checkup);
        
            // Convert back to DTO for response
            CheckupDTO responseDTO = convertToDTO(savedCheckup);
        
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating checkup: " + e.getMessage());
        }
    }

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
            
            // Check if plan is complete
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
            
            checkup.setDemandeRecomptage(true);
            checkup.setJustificationRecomptage(request.getJustification());
            checkupRepository.save(checkup);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
/**
 * Converts a CheckupDTO to a Checkup entity
 */
private Checkup convertToEntity(CheckupDTO dto) {
    Checkup checkup = new Checkup();
    
    // Set basic properties
    checkup.setId(dto.getId());
    checkup.setType(dto.getType());
    checkup.setValide(dto.isValide());
    checkup.setDemandeRecomptage(dto.isDemandeRecomptage());
    checkup.setJustificationRecomptage(dto.getJustificationRecomptage());
    
    // Set date if provided, otherwise use current time
    checkup.setDateCheck(dto.getDateCheck() != null ? dto.getDateCheck() : LocalDateTime.now());
    
    // Set agent
    if (dto.getAgent() != null && dto.getAgent().getId() != null) {
        AgentInventaire agent = agentInventaireRepository.findById(dto.getAgent().getId())
            .orElseThrow(() -> new EntityNotFoundException("Agent not found with ID: " + dto.getAgent().getId()));
        checkup.setAgent(agent);
    }
    
    // Set plan
    if (dto.getPlan() != null && dto.getPlan().getId() != null) {
        PlanInventaire plan = planInventaireRepository.findById(dto.getPlan().getId())
            .orElseThrow(() -> new EntityNotFoundException("Plan not found with ID: " + dto.getPlan().getId()));
        checkup.setPlan(plan);
    }
    
    // Set details
    if (dto.getDetails() != null && !dto.getDetails().isEmpty()) {
        List<CheckupDetail> details = new ArrayList<>();
        
        for (CheckupDTO.CheckupDetailDTO detailDTO : dto.getDetails()) {
            CheckupDetail detail = new CheckupDetail();
            detail.setId(detailDTO.getId());
            detail.setScannedQuantity(detailDTO.getScannedQuantity());
            detail.setManualQuantity(detailDTO.getManualQuantity());
            detail.setCheckup(checkup);
            
            // Set produit for each detail
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

/**
 * Converts a Checkup entity to a CheckupDTO
 */
private CheckupDTO convertToDTO(Checkup entity) {
    CheckupDTO dto = new CheckupDTO();
    
    // Set basic properties
    dto.setId(entity.getId());
    dto.setType(entity.getType());
    dto.setDateCheck(entity.getDateCheck());
    dto.setValide(entity.isValide());
    dto.setDemandeRecomptage(entity.isDemandeRecomptage());
    dto.setJustificationRecomptage(entity.getJustificationRecomptage());
    
    // Set agent
    if (entity.getAgent() != null) {
        CheckupDTO.AgentDTO agentDTO = new CheckupDTO.AgentDTO();
        agentDTO.setId(entity.getAgent().getId());
        agentDTO.setNom(entity.getAgent().getNom());
        agentDTO.setPrenom(entity.getAgent().getPrenom());
        agentDTO.setEmail(entity.getAgent().getEmail());
        dto.setAgent(agentDTO);
    }
    
    // Set plan
    if (entity.getPlan() != null) {
        CheckupDTO.PlanDTO planDTO = new CheckupDTO.PlanDTO();
        planDTO.setId(entity.getPlan().getId());
        planDTO.setNom(entity.getPlan().getNom());
        planDTO.setDateDebut(entity.getPlan().getDateDebut());
        planDTO.setDateFin(entity.getPlan().getDateFin());
        dto.setPlan(planDTO);
    }
    
    // Set details
    if (entity.getDetails() != null && !entity.getDetails().isEmpty()) {
        List<CheckupDTO.CheckupDetailDTO> detailDTOs = new ArrayList<>();
        
        for (CheckupDetail detail : entity.getDetails()) {
            CheckupDTO.CheckupDetailDTO detailDTO = new CheckupDTO.CheckupDetailDTO();
            detailDTO.setId(detail.getId());
            detailDTO.setScannedQuantity(detail.getScannedQuantity());
            detailDTO.setManualQuantity(detail.getManualQuantity());
            
            // Set produit for each detail
            if (detail.getProduit() != null) {
                CheckupDTO.ProduitDTO produitDTO = new CheckupDTO.ProduitDTO();
                produitDTO.setId(detail.getProduit().getId());
                produitDTO.setCodeBarre(detail.getProduit().getCodeBarre());
                produitDTO.setReference(detail.getProduit().getReference());
                produitDTO.setNom(detail.getProduit().getNom());
                produitDTO.setDescription(detail.getProduit().getDescription());
                produitDTO.setQuantitetheo(detail.getProduit().getQuantitetheo());
                
                // Set category and subcategory if available
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
            
            detailDTOs.add(detailDTO);
        }
        
        dto.setDetails(detailDTOs);
    }
    
    return dto;
}
@GetMapping("plan/{id}")
public ResponseEntity<PlanInventaireDTO> getPlanById(@PathVariable Long id) {
    return planInventaireRepository.findById(id)
        .map(this::convertToDTO)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
}

private PlanInventaireDTO convertToDTO(PlanInventaire plan) {
    PlanInventaireDTO dto = new PlanInventaireDTO();
    
    // Basic properties
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
    
    // Convert produits (simplified)
    if (plan.getProduits() != null) {
        plan.getProduits().forEach(produit -> {
            PlanInventaireDTO.ProduitDTO produitDTO = new PlanInventaireDTO.ProduitDTO();
            produitDTO.setId(produit.getId());
            
            // Add category if available
            if (produit.getCategory() != null) {
                PlanInventaireDTO.CategoryDTO categoryDTO = new PlanInventaireDTO.CategoryDTO();
                categoryDTO.setId(produit.getCategory().getId());
                categoryDTO.setName(produit.getCategory().getName());
                produitDTO.setCategoryDTO(categoryDTO);
            }
            
            // Add subcategory if available
            if (produit.getSubCategory() != null) {
                PlanInventaireDTO.SubCategoryDTO subCategoryDTO = new PlanInventaireDTO.SubCategoryDTO();
                subCategoryDTO.setId(produit.getSubCategory().getId());
                subCategoryDTO.setName(produit.getSubCategory().getName());
                produitDTO.setSubCategoryDTO(subCategoryDTO);
            }
            
            dto.getProduits().add(produitDTO);
        });
    }
    
    // Convert creator
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
}
}