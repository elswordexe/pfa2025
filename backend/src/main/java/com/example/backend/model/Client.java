package com.example.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue("CLIENT")
public class Client extends Utilisateur{
    public Client() {
        super();
        setRole(Role.CLIENT);
    }
    @OneToMany(mappedBy = "client")
    private List<AdministrateurClient> administrateurs;
    
    @OneToMany(mappedBy = "client")
    private List<AgentInventaire> agents;
}