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
        checkup.setValide(false);
        CheckupDetail detail = new CheckupDetail();
        detail.setProduit(produit);
        detail.setCheckup(checkup);
        detail.setScannedQuantity(1);
        detail.setType(CheckupType.SCAN);
        checkup.getDetails().add(detail);

        checkupRepository.save(checkup);
    }

    public List<Checkup> findByPlanAndType(Long planId, CheckupType type) {
        List<Checkup> allCheckups = checkupRepository.findByPlanId(planId);
        // Only keep checkups that have at least one detail of the requested type
        return allCheckups.stream()
            .filter(checkup -> checkup.getDetails() != null &&
                checkup.getDetails().stream().anyMatch(detail -> detail.getType() == type))
            .toList();
    }

}

