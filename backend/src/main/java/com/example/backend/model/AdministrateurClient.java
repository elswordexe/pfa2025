package com.example.backend.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue("ADMIN_CLIENT")
public class AdministrateurClient extends Utilisateur {
    
    @ManyToOne
    private Client client;
    
    public AdministrateurClient() {
        setRole(Role.ADMIN_CLIENT);
    }
}