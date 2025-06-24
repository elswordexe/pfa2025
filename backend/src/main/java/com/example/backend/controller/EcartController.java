package com.example.backend.controller;


import com.example.backend.model.Ecart;
import com.example.backend.model.PlanInventaire;
import com.example.backend.repository.EcartRepository;
import com.example.backend.repository.PlanInventaireRepository;
import com.example.backend.service.EcartService;
import com.example.backend.util.ExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/ecarts")
public class EcartController {
    @Autowired
    private PlanInventaireRepository planInventaireRepository;
    @Autowired
    private EcartRepository ecartRepository;
    
    @Autowired
    private PlanInventaireRepository planRepository;

    @Autowired
    private EcartService ecartService;

    @PostMapping("/generate/{planId}")
    public ResponseEntity<?> generateEcarts(@PathVariable Long planId) {
        try {
            ecartService.genererEcartsPourPlan(planId);
            return ResponseEntity.ok("Écarts générés avec succès.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur : " + e.getMessage());
        }
    }


    @PutMapping("/{ecartId}/valider")
    public ResponseEntity<?> validerEcart(@PathVariable Long ecartId) {
        try {
            Ecart ecart = ecartRepository.findById(ecartId)
                .orElseThrow(() -> new RuntimeException("Écart non trouvé"));
            ecartRepository.save(ecart);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la validation");
        }
    }

    @PutMapping("/{ecartId}/recomptage")
    public ResponseEntity<?> demanderRecomptage(@PathVariable Long ecartId) {
        try {
            Ecart ecart = ecartRepository.findById(ecartId)
                .orElseThrow(() -> new RuntimeException("Écart non trouvé"));
            ecart.setDemandeRecomptage(true);
            ecartRepository.save(ecart);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la demande de recomptage");
        }
    }


    @GetMapping("/export/xlsx")
    public void exportEcartsExcel(
            @RequestParam Long planId,
            HttpServletResponse response) throws IOException {
        try {
            PlanInventaire plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan non trouvé"));
            List<Ecart> ecarts = ecartRepository.findByPlanInventaireId(planId);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", 
                "attachment; filename=ecarts_" + plan.getNom() + ".xlsx");

            ExportUtil.EcartExcelExportUtils excelExporter = 
                new ExportUtil.EcartExcelExportUtils(ecarts, plan);
            excelExporter.exportDataToExcel(response);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Erreur lors de l'export Excel: " + e.getMessage());
        }
    }
}

