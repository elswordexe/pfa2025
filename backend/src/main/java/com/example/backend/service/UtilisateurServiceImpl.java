package com.example.backend.service;

import com.example.backend.model.*;
import com.example.backend.repository.ClientRepository;
import com.example.backend.repository.EmailVerificationTokenRepository;
import com.example.backend.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;


@Service
public class UtilisateurServiceImpl implements UtilisateurService {
    @Autowired
    private EmailVerificationTokenRepository tokenRepository;
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    
    @Autowired
    private ClientRepository clientRepository;
    
    @Override
    public Utilisateur registerUtilisateur(Utilisateur utilisateur) {
        if (utilisateurRepository.findByEmail(utilisateur.getEmail()).isPresent()) {
            throw new RuntimeException("L'email est déjà utilisé");
        }
        utilisateur.setPassword(passwordEncoder.encode(utilisateur.getPassword()));
        return utilisateurRepository.save(utilisateur);
    }

    @Override
    public Utilisateur createAdminClient(AdministrateurClient admin, Long clientId) {
        if (utilisateurRepository.findByEmail(admin.getEmail()).isPresent()) {
            throw new RuntimeException("L'email est déjà utilisé");
        }
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        admin.setClient(client);
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        return utilisateurRepository.save(admin);
    }

    @Override
    public Utilisateur createAgentInventaire(AgentInventaire agent, Long clientId) {
        if (utilisateurRepository.findByEmail(agent.getEmail()).isPresent()) {
            throw new RuntimeException("L'email est déjà utilisé");
        }
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        agent.setClient(client);
        agent.setPassword(passwordEncoder.encode(agent.getPassword()));
        return utilisateurRepository.save(agent);
    }
    
    @Override
    public Utilisateur findByEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
    @Override
    public boolean checkPassword(Utilisateur utilisateur, String password) {
        return passwordEncoder.matches(password, utilisateur.getPassword());
    }

    @Override
    public Utilisateur verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Token invalide"));

        if (verificationToken.getExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expiré");
        }

        Utilisateur user = verificationToken.getUtilisateur();
        tokenRepository.delete(verificationToken);
        return utilisateurRepository.save(user);
    }

    @Override
    public void initiatePasswordReset(String email) {
        try {
            Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            String token = UUID.randomUUID().toString();
            user.setResetPasswordToken(token);
            user.setResetPasswordTokenExpiryDate(LocalDateTime.now().plusHours(24));
            utilisateurRepository.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), token);

        } catch (Exception e) {

            throw new RuntimeException("Erreur lors de l'envoi de l'email de réinitialisation");
        }
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        Utilisateur user = utilisateurRepository.findByResetPasswordToken(token)
            .orElseThrow(() -> new RuntimeException("Token invalide"));

        if (user.getResetPasswordTokenExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expiré");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiryDate(null);
        utilisateurRepository.save(user);
    }
}