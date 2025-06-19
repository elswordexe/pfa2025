package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codeBarre;
    private String reference;
    private String nom;
    private String description;
    private Double prix;
    private String unite;
    private LocalDateTime datecremod;
    private String imageUrl;    private Integer quantitetheo;
    private String status;

    @ManyToOne
    @JoinColumn(name = "category_id")
    @JsonIgnoreProperties({"subCategories", "produits"})
    private Category category;

    @ManyToOne
    @JoinColumn(name = "sub_category_id")
    @JsonIgnoreProperties({"category", "produits"})
    private SubCategory subCategory;

    @ManyToMany(mappedBy = "produits")
    @JsonIgnoreProperties({"produits", "plans", "zoneProduits"})
    private List<Zone> zones = new ArrayList<>();

    @ManyToMany(mappedBy = "produits")
    @JsonIgnoreProperties({"zones", "produits", "assignations", "createur"})
    private List<PlanInventaire> plans = new ArrayList<>();

    @OneToMany(mappedBy = "produit", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"produit", "zone"})
    private Set<ZoneProduit> zoneProduits = new HashSet<>();

    // Methods omitted for brevity
}