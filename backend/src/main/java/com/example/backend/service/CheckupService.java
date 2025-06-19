package com.example.backend.service;

import com.example.backend.model.*;
import com.example.backend.repository.AgentInventaireRepository;
import com.example.backend.repository.CheckupRepository;
import com.example.backend.repository.PlanInventaireRepository;
import com.example.backend.repository.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CheckupService {
    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private CheckupRepository checkupRepository;

    @Autowired
    private AgentInventaireRepository agentInventaireRepository;
    @Autowired
    private PlanInventaireRepository planInventaireRepository;

    public void ajouterProduitParScan(Long agentId, Long planId, String CodeBare) {
        Produit produit = produitRepository.findByCodeBarre(CodeBare)
                .orElseGet(() -> {
                    Produit nouveauProduit = new Produit();
                    nouveauProduit.setCodeBarre(CodeBare);
                    nouveauProduit.setNom("Produit inconnu");
                    nouveauProduit.setDatecremod(LocalDateTime.now());
                    return produitRepository.save(nouveauProduit);
                });

        AgentInventaire agent = agentInventaireRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent introuvable"));

        PlanInventaire plan = planInventaireRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan introuvable"));

        Checkup checkup = new Checkup();
        checkup.setAgent(agent);
        checkup.setPlan(plan);
        checkup.setDateCheck(LocalDateTime.now());
        for(CheckupDetail detail: checkup.getDetails() ) {
            detail.setProduit(produit);
            detail.setCheckup(checkup);
            detail.setScannedQuantity(1);
        }
        checkup.setType(CheckupType.SCAN);
        checkup.setValide(false);

        checkupRepository.save(checkup);
    }

    public List<Checkup> findByPlanAndType(Long planId, CheckupType type) {
        return checkupRepository.findByPlanIdAndType(planId, type);
    }

}

