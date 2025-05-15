package com.example.backend.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("AgentInventaire")

public class AgentInventaire extends Utilisateur {

    public AgentInventaire() {
        super();
    }
    public AgentInventaire(String nom, String prenom, String email) {
        super(nom, prenom, email);
    }
    public AgentInventaire(String nom, String prenom, String email, String password) {
        super(nom, prenom, email, password);
    }
}