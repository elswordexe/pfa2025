package com.example.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nom;
    private String adresse;
    private String telephone;
    private String email;
    
    @OneToMany(mappedBy = "client")
    private List<AdministrateurClient> administrateurs;
    
    @OneToMany(mappedBy = "client")
    private List<AgentInventaire> agents;
}