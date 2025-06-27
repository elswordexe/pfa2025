package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "plan_inventaires")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PlanInventaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    private LocalDateTime dateDebut;

    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    private TYPE type;

    @Enumerated(EnumType.STRING)
    private RECCURENCE recurrence;

    @Enumerated(EnumType.STRING)
    private STATUS statut = STATUS.Indefini;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "plan_zones",
        joinColumns = @JoinColumn(name = "plan_id"),
        inverseJoinColumns = @JoinColumn(name = "zone_id")
    )
    private Set<Zone> zones = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "plan_produits",
        joinColumns = @JoinColumn(name = "plan_id"),
        inverseJoinColumns = @JoinColumn(name = "produit_id")
    )
    private Set<Produit> produits = new HashSet<>();

    private boolean inclusTousProduits = true;

    @OneToMany(mappedBy = "planInventaire", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<AssignationAgent> assignations = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "createur_id")
    private Utilisateur createur;

    @ManyToOne
    @JoinColumn(name = "zone_id")
    private Zone zone;

    private LocalDateTime dateCreation = LocalDateTime.now();


}