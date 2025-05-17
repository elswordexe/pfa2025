package com.example.backend.model;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false)
    private Integer quantiteTheorique;

    @Column(nullable = false)
    private Integer quantiteComptee;

    @Column(nullable = false)
    private Integer ecartQuantite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EcartType type;

    private String justification;

    @Column(nullable = false)
    private boolean valide;

    private boolean demandeRecomptage;
}