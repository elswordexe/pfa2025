package com.example.backend.controller;

import com.example.backend.model.PlanInventaire;
import com.example.backend.repository.PlanInventaireRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class PlanInventaireController {
    @Autowired
    private PlanInventaireRepository planInventaireRepository;

    @GetMapping("Plans")
    public Iterable<PlanInventaire> getAllPlans() {
        return planInventaireRepository.findAll();
    }

    @PostMapping("Plan/ajout")
    public ResponseEntity<?> ajouterPlan(@RequestBody PlanInventaire planInventaire) {
        PlanInventaire plan = planInventaireRepository.save(planInventaire);
        return ResponseEntity.ok(plan);
    }

    @DeleteMapping("Plan/{planId}")
    public ResponseEntity<?> supprimerPlan(@PathVariable Long planId) {
        planInventaireRepository.deleteById(planId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("Plan/{planId}")
    public ResponseEntity<?> modifierPlan(@PathVariable Long planId, @RequestBody PlanInventaire planInventaire) {
        if (!planId.equals(planInventaire.getId())) {
            return ResponseEntity.badRequest().build();
        }
        PlanInventaire plan = planInventaireRepository.save(planInventaire);
        return ResponseEntity.ok(plan);
    }
}