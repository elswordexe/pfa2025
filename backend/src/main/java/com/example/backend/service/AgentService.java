package com.example.backend.service;
import com.example.backend.model.AgentInventaire;
import com.example.backend.model.AssignationAgent;
import com.example.backend.model.PlanInventaire;
import com.example.backend.model.Zone;
import com.example.backend.repository.AssignationAgentRepository;
import com.example.backend.repository.PlanInventaireRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.repository.ZoneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AgentService {
    @Autowired
    AssignationAgentRepository agentRepository;
    @Autowired
    private ZoneRepository zoneRepository;
    @Autowired
    private AssignationAgentRepository assignationAgentRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PlanInventaireRepository planInventaireRepository;

    public void assignAgentToPlan(Long agentId, Long planId) {
        AgentInventaire agent = (AgentInventaire) utilisateurRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

        PlanInventaire plan = planInventaireRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan non trouvé"));

        AssignationAgent assignation = new AssignationAgent();
        assignation.setAgent(agent);
        assignation.setPlanInventaire(plan);
        assignation.setDateAssignation(LocalDateTime.now());

        assignationAgentRepository.save(assignation);
    }

    public void assignAgentToPlan(Long agentId, Long planId, Long zoneId) throws Exception {
        AgentInventaire agent = utilisateurRepository.findById(agentId)
                .filter(a -> a instanceof AgentInventaire)
                .map(a -> (AgentInventaire) a)
                .orElseThrow(() -> new Exception("Agent non trouvé"));

        PlanInventaire plan = planInventaireRepository.findById(planId)
                .orElseThrow(() -> new Exception("Plan non trouvé"));

        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new Exception("Zone non trouvée"));

        AssignationAgent assignation = new AssignationAgent();
        assignation.setAgent(agent);
        assignation.setPlanInventaire(plan);
        assignation.setZone(zone);
        assignation.setDateAssignation(LocalDateTime.now());

        assignationAgentRepository.save(assignation);
    }
}