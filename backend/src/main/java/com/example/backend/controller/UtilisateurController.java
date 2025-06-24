package com.example.backend.controller;

import com.example.backend.model.*;
import com.example.backend.repository.EmailVerificationTokenRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.service.AgentService;
import com.example.backend.service.EmailService;
import com.example.backend.service.UtilisateurService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.example.backend.service.JwtService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

@RestController
@CrossOrigin(origins = "*")
@Tag(name = "Utilisateur Management", description = "APIs pour manager les utilisateurs")
public class UtilisateurController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;
    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;


    @Autowired
    private UtilisateurRepository utilisateurRepository;
    
    @Autowired
    private UtilisateurService utilisateurService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Operation(summary = "Lister tous les utilisateurs")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",description = "lister tous les utilisateurs")})
    @GetMapping("users")
    public List<Utilisateur> getAllUtilisateurs() {
        return (List<Utilisateur>) utilisateurRepository.findAll();
    }
    @Operation(summary = "Lister tous les Agents inventaires")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",description = "lister tous les AG.Inv")})
    @GetMapping("AgentInventaire")
    public List<Utilisateur> getAllAgentsInventaires() {
        return  utilisateurRepository.findByRole(Role.AGENT_INVENTAIRE);
    }
    @PostMapping("users/register")
    public ResponseEntity<?> registerUtilisateur(@RequestBody Utilisateur utilisateur) {
        try {
            System.out.println("Registering user: " + utilisateur.getEmail());

            utilisateur.setPassword(passwordEncoder.encode(utilisateur.getPassword()));
            utilisateurRepository.save(utilisateur);

            String token = UUID.randomUUID().toString();
            System.out.println("Generated token: " + token);

            EmailVerificationToken verificationToken = new EmailVerificationToken();
            verificationToken.setToken(token);
            verificationToken.setUtilisateur(utilisateur);
            verificationToken.setExpiration(LocalDateTime.now().plusDays(1));
            
            EmailVerificationToken savedToken = tokenRepository.save(verificationToken);
            System.out.println("Saved token ID: " + savedToken.getId());

            // Send verification email
            emailService.sendVerificationEmail(utilisateur.getEmail(), token);
            System.out.println("Verification email sent to: " + utilisateur.getEmail());

            return ResponseEntity.ok(Map.of(
                "message", "Utilisateur enregistré. Veuillez vérifier votre email pour activer le compte.",
                "user", utilisateur,
                "debugToken", token
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    @GetMapping("/users/check-token")
public ResponseEntity<?> checkToken(@RequestParam("token") String token) {
    try {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Token non trouvé"));
        }

        EmailVerificationToken verificationToken = tokenOpt.get();
        Utilisateur utilisateur = verificationToken.getUtilisateur();

        return ResponseEntity.ok(Map.of(
            "valid", true,
            "email", utilisateur.getEmail()
        ));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", e.getMessage()));
    }
}

@GetMapping("/users/validate")
public ResponseEntity<?> verifyAccount(@RequestParam("token") String token) {
    try {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Lien de vérification invalide."));
        }

        EmailVerificationToken verificationToken = tokenOpt.get();
        Utilisateur utilisateur = verificationToken.getUtilisateur();

        if (utilisateur.isEnabled()) {
            return ResponseEntity.ok()
                .body(Map.of(
                    "message", "Compte déjà vérifié !",
                    "email", utilisateur.getEmail()
                ));
        }
        utilisateurRepository.save(utilisateur);

        // Only delete token after successful verification
        tokenRepository.delete(verificationToken);

        return ResponseEntity.ok()
            .body(Map.of(
                "message", "Compte vérifié avec succès !",
                "email", utilisateur.getEmail()
            ));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", "Erreur lors de la vérification: " + e.getMessage()));
    }
}
    @Autowired
    private AgentService agentService;

    @PostMapping("AgentInventaire/assign/{planId}/{agentId}")
    public ResponseEntity<?> assignAgentToPlan(@PathVariable("planId") Long planId,
                                               @PathVariable("agentId") Long agentId) {
        try {
            agentService.assignAgentToPlan(agentId, planId);
            return ResponseEntity.ok("Agent assigné au plan avec succès.");
        } catch (Exception e) {
            e.printStackTrace(); // utile pour debug
            return ResponseEntity.badRequest().body("Erreur : " + e.getMessage());
        }
    }
    @PostMapping("AgentInventaire/assign/{planId}/{agentId}/{zoneId}")
    public ResponseEntity<?> assignAgentToPlan(
            @PathVariable("planId") Long planId,
            @PathVariable("agentId") Long agentId,
            @PathVariable("zoneId") Long zoneId) {
        try {
            agentService.assignAgentToPlan(agentId, planId, zoneId);
            return ResponseEntity.ok("Agent assigné au plan et à la zone avec succès.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erreur : " + e.getMessage());
        }
    }

    @Operation(summary = "Créer un compte administrateur client", description = "Crée un compte admin pour un client spécifique")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Admin client créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou client non trouvé")
    })
    @PostMapping("users/client-admin/{clientId}")
    public ResponseEntity<?> createClientAdmin(@PathVariable Long clientId, @RequestBody AdministrateurClient admin) {
        try {
            Utilisateur user = utilisateurService.createAdminClient(admin, clientId);
            return ResponseEntity.ok(Map.of(
                "message", "Administrateur client créé avec succès",
                "user", user
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    
    @Operation(summary = "Créer un compte agent d'inventaire", description = "Crée un compte agent pour un client spécifique")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Agent créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou client non trouvé")
    })
    @PostMapping("users/agent-inventaire/{clientId}")
    public ResponseEntity<?> createInventoryAgent(@PathVariable Long clientId, @RequestBody AgentInventaire agent) {
        try {
            Utilisateur user = utilisateurService.createAgentInventaire(agent, clientId);
            return ResponseEntity.ok(Map.of(
                "message", "Agent d'inventaire créé avec succès",
                "user", user
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "connexion d utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "connexion réussie"),
            @ApiResponse(responseCode = "400", description = "erreur de connexion")
    })
    @PostMapping("users/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            Optional<Utilisateur> userOpt = utilisateurRepository.findByEmail(email);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Email ou mot de passe incorrect"));
            }

            Utilisateur user = userOpt.get();
            String jwtToken = jwtService.generateToken(user);
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("nom", user.getNom());
            userMap.put("prenom", user.getPrenom());
            userMap.put("email", user.getEmail());
            userMap.put("role", user.getRole() != null ? user.getRole() : Role.Utilisateur);
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("message", "Connexion réussie");
            responseMap.put("user", userMap);
            responseMap.put("token", jwtToken);

            return ResponseEntity.ok(responseMap);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Email ou mot de passe incorrect"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la connexion: " + e.getMessage()));
        }
    }

    @Operation(summary="lister nombres d'Utilisateur")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "nombre")
    })
    @GetMapping("users/count")
    public ResponseEntity<Integer> getCountUtilisateurs() {
        return ResponseEntity.ok((int) utilisateurRepository.count());
    }

    @Operation(summary="Compter le nombre d'agents d'inventaire")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nombre d'agents d'inventaire")
    })
    @GetMapping("users/countAgentInventaire")
    public ResponseEntity<Integer> getCountAgentInventaire() {
        long count = utilisateurRepository.countByRole(Role.AGENT_INVENTAIRE);
        return ResponseEntity.ok((int) count);
    }
    @Operation(summary="Compter le nombre d'administrateur Client")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nombre d'administrateur Clients")
    })
    @GetMapping("users/countAdminClient")
    public ResponseEntity<Integer> getCountAdminClient() {
        long count = utilisateurRepository.countByRole(Role.ADMIN_CLIENT);
        return ResponseEntity.ok((int) count);
    }
    @Operation(summary = "déconnexion d'utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "déconnexion réussie")
    })
    @PostMapping("users/logout")
    public ResponseEntity<?> logoutUser() {
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }
    @Operation(summary = "Obtenir les noms et dates de modification des utilisateurs")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès"),
        @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des données")
    })
    @GetMapping("users/names-dates")
    public ResponseEntity<List<Map<String, Object>>> getUsersNameAndDate(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        try {
        Pageable pageable = PageRequest.of(page, size);
        Page<Utilisateur> userPage = utilisateurRepository.findAll(pageable);
        
        List<Map<String, Object>> usersData = userPage.getContent().stream()
                .map(user -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", user.getId());
                    data.put("name", user.getNom());
                    data.put("date", user.getDatecremod());
                    return data;
                })
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(usersData);
        } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(List.of());
        }
    }
    @Operation(summary = "Delete a User")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deletion cmplete"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la suppression ")
    })
    @DeleteMapping("users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        utilisateurRepository.deleteById(id);
    return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé"));
    }

    @Operation(summary = "Modification d'Utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modification avec succès"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la modification des données")
    })
    @PutMapping("users/{id}")
    public ResponseEntity<?> modifyUser(@PathVariable Long id, @RequestBody Utilisateur userDetails) {
        return utilisateurRepository.findById(id).map(user -> {
            user.setNom(userDetails.getNom());
            user.setPrenom(userDetails.getPrenom());
            user.setEmail(userDetails.getEmail());
            user.setUsername(userDetails.getUsername());
            user.setTelephone(userDetails.getTelephone());
            if (userDetails.getRole() != null) {
                user.setRole(userDetails.getRole());
            }

            utilisateurRepository.save(user);
            return ResponseEntity.ok(user);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/verify")
    @Operation(summary = "Vérifier l'email d'un utilisateur")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email vérifié avec succès"),
        @ApiResponse(responseCode = "400", description = "Token invalide ou expiré")
    })
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            Utilisateur user = utilisateurService.verifyEmail(token);
            return ResponseEntity.ok(Map.of(
                "message", "Email vérifié avec succès",
                "email", user.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Erreur de vérification: " + e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Demander la réinitialisation du mot de passe")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email de réinitialisation envoyé"),
        @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            utilisateurService.initiatePasswordReset(email);
            return ResponseEntity.ok(Map.of(
                "message", "Si l'email existe, un lien de réinitialisation a été envoyé"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Erreur: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Réinitialiser le mot de passe")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mot de passe réinitialisé avec succès"),
        @ApiResponse(responseCode = "400", description = "Token invalide ou expiré")
    })
    public ResponseEntity<?> resetPassword(
            @RequestParam String token,
            @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("password");
            utilisateurService.resetPassword(token, newPassword);
            return ResponseEntity.ok(Map.of(
                "message", "Mot de passe réinitialisé avec succès"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Erreur de réinitialisation: " + e.getMessage()));
        }
    }


    @GetMapping("/users/agents")
    @Operation(summary = "Récupérer tous les agents d'inventaire")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des agents récupérée avec succès")
    })
    public ResponseEntity<List<Map<String, Object>>> getAllAgents() {
    List<Utilisateur> agents = utilisateurRepository.findByRole(Role.AGENT_INVENTAIRE);
    
    List<Map<String, Object>> agentsData = agents.stream()
        .map(agent -> {
            Map<String, Object> data = new HashMap<>();
            data.put("id", agent.getId());
            data.put("firstName", agent.getPrenom());
            data.put("lastName", agent.getNom());
            data.put("email", agent.getEmail());
            return data;
        })
        .collect(Collectors.toList());
    
    return ResponseEntity.ok(agentsData);
    }
}