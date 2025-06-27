package com.example.backend.repository;

import com.example.backend.model.AssignationAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignationAgentRepository extends JpaRepository<AssignationAgent, Long> {
    
    @Query("SELECT DISTINCT a FROM AssignationAgent a " +
           "LEFT JOIN FETCH a.agent " +
           "LEFT JOIN FETCH a.zone " +
           "LEFT JOIN FETCH a.planInventaire " +
           "WHERE a.planInventaire.id = :planId")
    List<AssignationAgent> findByPlanInventaireId(@Param("planId") Long planId);

    List<AssignationAgent> findByAgentId(Long agentId);
}