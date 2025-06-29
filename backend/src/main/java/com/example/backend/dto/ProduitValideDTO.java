package com.example.backend.dto;

import java.time.LocalDateTime;

public class ProduitValideDTO {
    private Long produitId;
    private Long zoneId;
    private Long planId;
    private Double quantiteTheorique;
    private Double quantiteManuelle;
    private Double quantiteScan;
    private LocalDateTime dateValidation;

    public ProduitValideDTO() {
    }
    public Long getProduitId() {
        return produitId;
    }

    public void setProduitId(Long produitId) {
        this.produitId = produitId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
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