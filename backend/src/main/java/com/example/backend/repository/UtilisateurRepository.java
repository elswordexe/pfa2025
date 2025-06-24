package com.example.backend.repository;

import com.example.backend.model.Role;
import com.example.backend.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    List<Utilisateur> findByRole(Role role);
    long countByRole(Role role);
    Optional<Utilisateur> findByResetPasswordToken(String token);

    Optional<Utilisateur> findById(Long createurId);
}