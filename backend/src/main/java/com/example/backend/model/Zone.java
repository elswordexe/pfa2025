package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Zone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @ManyToMany
    @JoinTable(
        name = "zone_produit",
        joinColumns = @JoinColumn(name = "zone_id"),
        inverseJoinColumns = @JoinColumn(name = "produit_id")
    )
    @JsonIgnoreProperties({"zones", "plans", "zoneProduits"})
    private List<Produit> produits = new ArrayList<>();

    @ManyToMany(mappedBy = "zones")
    @JsonIgnoreProperties({"zones", "produits", "assignations", "createur"})
    private List<PlanInventaire> plans = new ArrayList<>();

    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"zone", "produit"})
    private Set<ZoneProduit> zoneProduits = new HashSet<>();

    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"zone", "planInventaire", "agent"})
    private List<AssignationAgent> assignations = new ArrayList<>();

    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"zone", "produit", "checkup"})
    private List<CheckupDetail> checkupDetails = new ArrayList<>();

}