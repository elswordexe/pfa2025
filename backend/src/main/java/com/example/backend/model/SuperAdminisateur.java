package com.example.backend.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("SuperAdminisateur")
public class SuperAdminisateur extends Utilisateur{
    public SuperAdminisateur() {
        super();
    }

}
