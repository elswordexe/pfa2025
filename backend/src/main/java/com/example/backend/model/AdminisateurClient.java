package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@DiscriminatorValue("AdminisateurClient")
@JsonTypeName("AdminisateurClient")
@Entity
public class AdminisateurClient extends Utilisateur{
    public AdminisateurClient() {
        super();
    }

}
