package com.example.backend.service;

import com.example.backend.model.AdministrateurClient;
import com.example.backend.model.AgentInventaire;
import com.example.backend.model.Client;
import com.example.backend.model.Utilisateur;
import com.example.backend.repository.ClientRepository;
import com.example.backend.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class UtilisateurServiceImpl implements UtilisateurService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;
    
    @Autowired
    private ClientRepository clientRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
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
}