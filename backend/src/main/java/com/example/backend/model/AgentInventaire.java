package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
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

    @JsonManagedReference(value = "agent-assignations")
    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL)
    private List<AssignationAgent> assignations;
    
    public AgentInventaire() {
        setRole(Role.AGENT_INVENTAIRE);
    }
}