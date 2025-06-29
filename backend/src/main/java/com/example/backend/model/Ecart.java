package com.example.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ecarts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ecart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "plan_inventaire_id")
    private PlanInventaire planInventaire;

    @ManyToOne
    private Produit produit;

   private  EcartType type;
    private Integer quantiteComptee;

    @Column(name = "ecart_quantite")
    private Integer ecartQuantite;

    private LocalDateTime dateCreation = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    protected void calculateEcart() {
        if (this.produit != null && this.quantiteComptee != null) {
            this.ecartQuantite = this.quantiteComptee - this.produit.getQuantitetheo();
        }
    }
    public Integer getQuantiteTheorique() {
        return this.produit != null ? this.produit.getQuantitetheo() : null;
    }

    @Enumerated(EnumType.STRING)
    private EcartStatut statut = EcartStatut.EN_ATTENTE;

    private String justification;
    private boolean demandeRecomptage = false;

    @ManyToOne
    private Utilisateur validateur;

    private LocalDateTime dateValidation;
}

