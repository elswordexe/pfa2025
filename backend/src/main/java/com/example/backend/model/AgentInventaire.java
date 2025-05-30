package com.example.backend.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue("AGENT_INVENTAIRE")
public class AgentInventaire extends Utilisateur {
    
    @ManyToOne
    private Client client;
    
    @OneToMany(mappedBy = "agent")
    private List<AssignationAgent> assignations;
    
    public AgentInventaire() {
        setRole(Role.AGENT_INVENTAIRE);
    }
}