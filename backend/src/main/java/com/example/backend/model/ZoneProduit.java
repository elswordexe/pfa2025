package com.example.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "zone_produit")
@Getter
@Setter
@NoArgsConstructor
public class ZoneProduit {
    @EmbeddedId
    private ZoneProduitId id;

    @ManyToOne
    @MapsId("zoneId")
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @ManyToOne
    @MapsId("produitId")
    @JoinColumn(name = "produit_id")
    private Produit produit;

    private Integer quantiteTheorique;
    private boolean verified = false;
}