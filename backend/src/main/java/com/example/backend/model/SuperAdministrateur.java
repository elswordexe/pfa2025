package com.example.backend.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue("SUPER_ADMIN")
public class SuperAdministrateur extends Utilisateur {
    
    public SuperAdministrateur() {
        setRole(Role.SUPER_ADMIN);
    }
}