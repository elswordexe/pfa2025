package com.example.backend.repository;

import com.example.backend.model.AssignationAgent;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface AssignationAgentRepository extends JpaRepository<AssignationAgent, Long> {
    List<AssignationAgent> findByPlanInventaireId(Long planId);
    List<AssignationAgent> findByAgentId(Long agentId);
}