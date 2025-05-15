package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "produits")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Produit {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    private String CodeBar;
    private String reference;
    private String nom;
    private String description;
    private String unite;
    private Double prix;
    @OneToMany(mappedBy = "produit")
    private List<Image> images;
    @JsonBackReference
    @ManyToMany(mappedBy = "produits")
    private List<Zone> zones = new ArrayList<>();
}