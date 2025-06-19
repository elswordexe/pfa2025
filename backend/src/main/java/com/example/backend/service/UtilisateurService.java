package com.example.backend.service;

import com.example.backend.model.AdministrateurClient;
import com.example.backend.model.AgentInventaire;
import com.example.backend.model.Utilisateur;

public interface UtilisateurService {
    Utilisateur registerUtilisateur(Utilisateur utilisateur);
    Utilisateur createAdminClient(AdministrateurClient admin, Long clientId);
    Utilisateur createAgentInventaire(AgentInventaire agent, Long clientId);
    Utilisateur findByEmail(String email);
    boolean checkPassword(Utilisateur utilisateur, String password);
    Utilisateur verifyEmail(String token);
    void initiatePasswordReset(String email);
    void resetPassword(String token, String newPassword);
}