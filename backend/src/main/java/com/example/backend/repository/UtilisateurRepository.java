package com.example.backend.repository;

import com.example.backend.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
}
