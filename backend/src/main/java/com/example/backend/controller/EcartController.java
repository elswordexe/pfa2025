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
}

