package com.example.backend.model;

import lombok.Getter;

@Getter
public enum EcartStatut {
    EN_ATTENTE("En attente"),
    VALIDE("Validé"),
    REJETE("Rejeté"),
    RECOMPTAGE("Recomptage demandé");

    private final String libelle;

    EcartStatut(String libelle) {
        this.libelle = libelle;
    }
}
