package com.example.backend.service;

import com.example.backend.model.*;
import com.example.backend.repository.CheckupRepository;
import com.example.backend.repository.EcartRepository;
import com.example.backend.repository.PlanInventaireRepository;
import com.example.backend.repository.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EcartService {

    @Autowired
    private EcartRepository ecartRepository;

    @Autowired
    private CheckupRepository checkupRepository;

    @Autowired
    private PlanInventaireRepository planInventaireRepository;

    @Autowired
    private ProduitRepository produitRepository;

    public List<Ecart> findAll() {
        return ecartRepository.findAll();
    }

    public void genererEcartsPourPlan(Long planId) {
        PlanInventaire plan = planInventaireRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan non trouvé"));

        List<Checkup> checkups = checkupRepository.findByPlanId(planId);

        Map<Produit, Integer> quantitesComptées = new HashMap<>();

        for (Checkup checkup : checkups) {
            for (CheckupDetail detail : checkup.getDetails()) {
                Produit produit = detail.getProduit();
                int scanned = detail.getScannedQuantity();
                quantitesComptées.put(
                        produit,
                        quantitesComptées.getOrDefault(produit, 0) + scanned
                );
            }
        }

        for (Map.Entry<Produit, Integer> entry : quantitesComptées.entrySet()) {
            Produit produit = entry.getKey();
            int qteComptee = entry.getValue();
            int qteTheorique = produit.getQuantitetheo();

            if (qteComptee != qteTheorique) {
                Ecart ecart = new Ecart();
                ecart.setQuantiteComptee(qteComptee);
                ecart.setEcartQuantite(qteComptee - qteTheorique);
                ecart.setType(qteComptee > qteTheorique ? EcartType.CRITIQUE : EcartType.MINEUR);
                ecart.setDemandeRecomptage(false);
                ecart.setProduit(produit);
                ecartRepository.save(ecart);
            }
        }
    }

    public List<Ecart> getEcartsParPlan(Long planId) {
        return ecartRepository.findByPlanInventaireId(planId);
    }

    @Transactional
    public Ecart createEcart(Ecart ecart) {
        int theoreticalQty = ecart.getProduit().getQuantitetheo();
        ecart.setEcartQuantite(ecart.getQuantiteComptee() - theoreticalQty);
        ecart.setDateCreation(LocalDateTime.now());
        ecart.setStatut(EcartStatut.EN_ATTENTE);

        return ecartRepository.save(ecart);
    }

    @Transactional
    public Ecart validateEcart(Long ecartId, Utilisateur validateur, String justification) {
        Ecart ecart = ecartRepository.findById(ecartId)
                .orElseThrow(() -> new RuntimeException("Écart non trouvé"));

        ecart.setStatut(EcartStatut.VALIDE);
        ecart.setValidateur(validateur);
        ecart.setJustification(justification);
        ecart.setDateValidation(LocalDateTime.now());

        if (ecart.getStatut() == EcartStatut.VALIDE) {
            var produit = ecart.getProduit();
            produit.setQuantitetheo(ecart.getQuantiteComptee());
            produitRepository.save(produit);
        }

        return ecartRepository.save(ecart);
    }

    @Transactional
    public Ecart demanderRecomptage(Long ecartId) {
        Ecart ecart = ecartRepository.findById(ecartId)
                .orElseThrow(() -> new RuntimeException("Écart non trouvé"));

        ecart.setStatut(EcartStatut.RECOMPTAGE);
        ecart.setDemandeRecomptage(true);

        return ecartRepository.save(ecart);
    }

    @Transactional(readOnly = true)
    public List<Ecart> getEcartsSignificatifs() {
        return ecartRepository.findSignificantEcarts();
    }

    @Transactional(readOnly = true)
    public List<Ecart> getEcartsByPlanAndStatus(Long planId, EcartStatut statut) {
        return ecartRepository.findByPlanInventaireIdAndStatut(planId, statut);
    }

    @Transactional(readOnly = true)
    public List<Ecart> getEcartsEnAttente() {
        return ecartRepository.findPendingEcartsOrderByMagnitude();
    }

    @Transactional
    public void rejeterEcart(Long ecartId, String justification) {
        Ecart ecart = ecartRepository.findById(ecartId)
                .orElseThrow(() -> new RuntimeException("Écart non trouvé"));

        ecart.setStatut(EcartStatut.REJETE);
        ecart.setJustification(justification);
        ecart.setDateValidation(LocalDateTime.now());

        ecartRepository.save(ecart);
    }
}
