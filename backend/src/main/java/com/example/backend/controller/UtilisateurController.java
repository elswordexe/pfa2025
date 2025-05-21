package com.example.backend.controller;

import com.example.backend.model.*;
import com.example.backend.repository.UtilisateurRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
@Tag(name = "Utilisateur Management", description = "APIs pour manager les utilisateurs")
public class UtilisateurController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

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
    
    @Operation(summary = "Enregistrer un nouvel utilisateur", description = "Crée un nouveau compte utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur enregistré avec succès"),
            @ApiResponse(responseCode = "400", description = "Données d'utilisateur invalides ou il existe deja")
    })
    @PostMapping("users/register")
    public ResponseEntity<?> registerUtilisateur(@RequestBody Utilisateur utilisateur) {
        try {
            Utilisateur user = utilisateurService.registerUtilisateur(utilisateur);
            return ResponseEntity.ok(Map.of(
                "message", "Utilisateur enregistré avec succès. Veuillez vérifier votre email.",
                "user", user
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
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
            // Authenticate with Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            // If authentication successful, find user
            Optional<Utilisateur> userOpt = utilisateurRepository.findByEmail(email);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Email ou mot de passe incorrect"));
            }

            // Generate JWT token
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
    public ResponseEntity<Long> getCountAgent() {
        return ResponseEntity.ok(utilisateurRepository.countByRole(Role.AGENT_INVENTAIRE));
    }
    @Operation(summary = "déconnexion d'utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "déconnexion réussie")
    })
    @PostMapping("users/logout")
    public ResponseEntity<?> logoutUser() {
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }
}