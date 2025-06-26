package com.example.backend.service;

import com.example.backend.dto.PlanInventaireDTO;
import com.example.backend.dto.ZoneDTO;
import com.example.backend.model.*;
import com.example.backend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@Transactional
@Slf4j
public class PlanInventaireService {
    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private PlanInventaireRepository planInventaireRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Transactional
    public PlanInventaire createPlan(PlanInventaireDTO planDTO) {
        try {
            PlanInventaire plan = new PlanInventaire();
            plan.setNom(planDTO.getNom());
            plan.setDateDebut(planDTO.getDateDebut());
            plan.setDateFin(planDTO.getDateFin());
            plan.setType(TYPE.valueOf(planDTO.getType()));
            plan.setStatut(STATUS.valueOf(planDTO.getStatut()));
            String recurrence = planDTO.getRecurrence();
            if (recurrence == null || recurrence.isEmpty()) {
                recurrence = "MENSUEL";
            }
            plan.setRecurrence(RECCURENCE.valueOf(recurrence));

            plan.setInclusTousProduits(TYPE.COMPLET.equals(plan.getType()));
            Set<Zone> zones = planDTO.getZones().stream()
                .map(zoneDTO -> zoneRepository.findById(zoneDTO.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + zoneDTO.getId())))
                .collect(Collectors.toSet());
            plan.setZones(zones);

            if (TYPE.COMPLET.equals(plan.getType())) {
                Set<Produit> allZoneProducts = zones.stream()
                    .flatMap(zone -> zone.getProduits().stream())
                    .collect(Collectors.toSet());
                plan.setProduits(allZoneProducts);
            } else if (planDTO.getProduits() != null && !planDTO.getProduits().isEmpty()) {
                Set<Produit> selectedProducts = planDTO.getProduits().stream()
                    .map(produitDTO -> produitRepository.findById(produitDTO.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + produitDTO.getId())))
                    .collect(Collectors.toSet());
                plan.setProduits(selectedProducts);
            }
            if (planDTO.getAssignations() != null) {
                Set<AssignationAgent> assignations = planDTO.getAssignations().stream()
                        .map(assignationDTO -> {
                            Zone zone = zoneRepository.findById(assignationDTO.getZone().getId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + assignationDTO.getZone().getId()));
                            AgentInventaire agent = (AgentInventaire) utilisateurRepository.findById(assignationDTO.getAgent().getId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + assignationDTO.getAgent().getId()));

                            AssignationAgent assignation = new AssignationAgent();
                            assignation.setZone(zone);
                            assignation.setAgent(agent);
                            assignation.setPlanInventaire(plan);
                            assignation.setDateAssignation(LocalDateTime.now());
                            return assignation;
                        }).collect(Collectors.toSet());

                plan.setAssignations(assignations);
            }
            PlanInventaire savedPlan = planInventaireRepository.save(plan);
            log.info("Created plan with ID: {}", savedPlan.getId());
            return savedPlan;

        } catch (Exception e) {
            log.error("Error creating plan", e);
            throw new RuntimeException("Error creating plan: " + e.getMessage());
        }
    }
}