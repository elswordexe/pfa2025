package com.example.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "produits_valides")
public class ProduitValide {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @ManyToOne
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanInventaire plan;

    @Column(nullable = false)
    private Double quantiteTheorique;

    @Column
    private Double quantiteManuelle;

    @Column
    private Double quantiteScan;

    @Column(nullable = false)
    private LocalDateTime dateValidation;

    public ProduitValide() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public PlanInventaire getPlan() {
        return plan;
    }

    public void setPlan(PlanInventaire plan) {
        this.plan = plan;
    }

    public Double getQuantiteTheorique() {
        return quantiteTheorique;
    }

    public void setQuantiteTheorique(Double quantiteTheorique) {
        this.quantiteTheorique = quantiteTheorique;
    }

    public Double getQuantiteManuelle() {
        return quantiteManuelle;
    }

    public void setQuantiteManuelle(Double quantiteManuelle) {
        this.quantiteManuelle = quantiteManuelle;
    }

    public Double getQuantiteScan() {
        return quantiteScan;
    }

    public void setQuantiteScan(Double quantiteScan) {
        this.quantiteScan = quantiteScan;
    }

    public LocalDateTime getDateValidation() {
        return dateValidation;
    }

    public void setDateValidation(LocalDateTime dateValidation) {
        this.dateValidation = dateValidation;
    }
} 