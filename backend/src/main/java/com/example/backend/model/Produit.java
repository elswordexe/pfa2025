package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Produit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String codeBar;
    private String reference;
    private String nom;
    
    @Column(length = 1000)
    private String description;
    
    private double prix;
    private String unite;
    private LocalDateTime datecremod;
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    
    @ManyToMany(mappedBy = "produits")
    @JsonIgnore
    private List<Zone> zones = new ArrayList<>();

}