package com.example.backend.controller;

import com.example.backend.model.*;
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;

@RestController
@CrossOrigin(origins = "*")
@Tag(name = "Utilisateur Management", description = "APIs pour manager les utilisateurs")
public class UtilisateurController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

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

    @Autowired
    private AgentService agentService;

    @Operation(
        summary = "Créer un utilisateur générique",
        description = "Crée un utilisateur de n'importe quel rôle (hors clientId obligatoire).\n\n"
            + "\u26a0\ufe0f Le corps (body) de la requête est OBLIGATOIRE et doit contenir les champs requis.\n\n"
            + "Le champ 'role' doit être l'une des valeurs suivantes : SUPER_ADMIN, ADMIN_CLIENT, AGENT_INVENTAIRE, Utilisateur, CLIENT.\n"
            + "Le champ 'dtype' doit correspondre à la classe Java : SuperAdministrateur, AdministrateurClient, AgentInventaire, Utilisateur, Client.\n"
            + "Le mot de passe et l'email sont obligatoires.\n\n"
            + "Exemple de correspondance :\n"
            + "- role: AGENT_INVENTAIRE, dtype: AgentInventaire\n"
            + "- role: ADMIN_CLIENT, dtype: AdministrateurClient\n"
            + "- role: SUPER_ADMIN, dtype: SuperAdministrateur\n"
            + "- role: Utilisateur, dtype: Utilisateur\n",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "\u26a0\ufe0f Ce body est OBLIGATOIRE. Fournir un objet JSON avec les champs requis : nom, prenom, email (valide), password (min 6 caractères), role, dtype. Voir exemples ci-dessous.",
            content = @Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    requiredProperties = {"email", "password", "role", "dtype"},
                    implementation = Utilisateur.class,
                    example = "{\n  \"nom\": \"Dupont\",\n  \"prenom\": \"Jean\",\n  \"email\": \"jean.dupont@example.com\",\n  \"password\": \"motdepasse123\",\n  \"role\": \"Utilisateur\",\n  \"dtype\": \"Utilisateur\"\n}"
                ),
                examples = {
                    @ExampleObject(
                        name = "Utilisateur",
                        value = "{\n  \"nom\": \"Dupont\",\n  \"prenom\": \"Jean\",\n  \"email\": \"jean.dupont@example.com\",\n  \"password\": \"motdepasse123\",\n  \"role\": \"Utilisateur\",\n  \"dtype\": \"Utilisateur\"\n}"
                    ),
                    @ExampleObject(
                        name = "Agent d'inventaire",
                        value = "{\n  \"nom\": \"Martin\",\n  \"prenom\": \"Paul\",\n  \"email\": \"paul.martin@example.com\",\n  \"password\": \"motdepasse123\",\n  \"role\": \"AGENT_INVENTAIRE\",\n  \"dtype\": \"AgentInventaire\"\n}"
                    ),
                    @ExampleObject(
                        name = "Administrateur client",
                        value = "{\n  \"nom\": \"Durand\",\n  \"prenom\": \"Alice\",\n  \"email\": \"alice.durand@example.com\",\n  \"password\": \"motdepasse123\",\n  \"role\": \"ADMIN_CLIENT\",\n  \"dtype\": \"AdministrateurClient\"\n}"
                    ),
                    @ExampleObject(
                        name = "Super administrateur",
                        value = "{\n  \"nom\": \"Boss\",\n  \"prenom\": \"Super\",\n  \"email\": \"super.boss@example.com\",\n  \"password\": \"motdepasse123\",\n  \"role\": \"SUPER_ADMIN\",\n  \"dtype\": \"SuperAdministrateur\"\n}"
                    )
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utilisateur créé avec succès",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Réponse utilisateur créé",
                    value = "{\n  \"id\": 10,\n  \"nom\": \"Dupont\",\n  \"prenom\": \"Jean\",\n  \"email\": \"jean.dupont@example.com\",\n  \"role\": \"Utilisateur\",\n  \"dtype\": \"Utilisateur\"\n}"
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Erreur lors de la création de l'utilisateur",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Erreur création",
                    value = "{\n  \"message\": \"Erreur création utilisateur: ...\"\n}"
                )
            )
        )
    })
    @PostMapping("users/register")
    public ResponseEntity<?> registerUser(@RequestBody Utilisateur user) {
        try {
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "L'email est obligatoire."));
            }
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le mot de passe est obligatoire."));
            }
            if (!user.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                return ResponseEntity.badRequest().body(Map.of("message", "Format d'email invalide."));
            }
            if (user.getPassword().length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le mot de passe doit contenir au moins 6 caractères."));
            }
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            if (user.getUsername() == null || user.getUsername().isEmpty()) {
                user.setUsername(user.getEmail());
            }
            if (user.getRole() == null) {
                user.setRole(Role.Utilisateur);
            }
            Utilisateur saved = utilisateurRepository.save(user);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Erreur création utilisateur: " + e.getMessage()));
        }
    }
    //test version login lwla
    @Operation(
        summary = "Créer un compte administrateur client",
        description = "Crée un compte admin pour un client spécifique",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Données de l'administrateur client à créer",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Exemple admin client",
                    value = "{\n  \"nom\": \"Durand\",\n  \"prenom\": \"Alice\",\n  \"email\": \"alice.durand@example.com\",\n  \"username\": \"alice.durand\",\n  \"telephone\": \"0601020304\",\n  \"role\": \"ADMIN_CLIENT\"\n}"
                )
            )
        )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Admin client créé avec succès",
                content = @Content(
                    examples = @ExampleObject(
                        name = "Réponse admin client créé",
                        value = "{\n  \"message\": \"Administrateur client créé avec succès\",\n  \"user\": {\n    \"id\": 5,\n    \"nom\": \"Durand\",\n    \"prenom\": \"Alice\",\n    \"email\": \"alice.durand@example.com\",\n    \"role\": \"ADMIN_CLIENT\"\n  }\n}"
                    )
                )
            ),
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

    @Operation(
        summary = "Créer un compte agent d'inventaire",
        description = "Crée un compte agent pour un client spécifique",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Données de l'agent à créer",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Exemple agent inventaire",
                    value = "{\n  \"nom\": \"Martin\",\n  \"prenom\": \"Paul\",\n  \"email\": \"paul.martin@example.com\",\n  \"username\": \"paul.martin\",\n  \"telephone\": \"0605060708\",\n  \"role\": \"AGENT_INVENTAIRE\"\n}"
                )
            )
        )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Agent créé avec succès",
                content = @Content(
                    examples = @ExampleObject(
                        name = "Réponse agent créé",
                        value = "{\n  \"message\": \"Agent d'inventaire créé avec succès\",\n  \"user\": {\n    \"id\": 6,\n    \"nom\": \"Martin\",\n    \"prenom\": \"Paul\",\n    \"email\": \"paul.martin@example.com\",\n    \"role\": \"AGENT_INVENTAIRE\"\n  }\n}"
                    )
                )
            ),
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

    @Operation(
        summary = "connexion d utilisateur",
        description = "Connexion d'un utilisateur avec email et mot de passe",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Identifiants de connexion",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Exemple login",
                    value = "{\n  \"email\": \"alice.durand@example.com\",\n  \"password\": \"motdepasse123\"\n}"
                )
            )
        )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "connexion réussie",
                content = @Content(
                    examples = @ExampleObject(
                        name = "Réponse connexion",
                        value = "{\n  \"message\": \"Connexion réussie\",\n  \"user\": {\n    \"id\": 5,\n    \"nom\": \"Durand\",\n    \"prenom\": \"Alice\",\n    \"email\": \"alice.durand@example.com\",\n    \"role\": \"ADMIN_CLIENT\"\n  },\n  \"token\": \"eyJhbGciOiJIUzI1NiJ9...\"\n}"
                    )
                )
            ),
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

    @Operation(
        summary = "Modification d'Utilisateur",
        description = "Modifier les informations d'un utilisateur existant.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Données à modifier",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Exemple modification utilisateur",
                    value = "{\n  \"nom\": \"Durand\",\n  \"prenom\": \"Alice\",\n  \"email\": \"alice.durand@example.com\",\n  \"username\": \"alice.durand\",\n  \"telephone\": \"0601020304\",\n  \"role\": \"ADMIN_CLIENT\"\n}"
                )
            )
        )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modification avec succès",
                content = @Content(
                    examples = @ExampleObject(
                        name = "Réponse modification utilisateur",
                        value = "{\n  \"id\": 5,\n  \"nom\": \"Durand\",\n  \"prenom\": \"Alice\",\n  \"email\": \"alice.durand@example.com\",\n  \"role\": \"ADMIN_CLIENT\"\n}"
                    )
                )
            ),
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
                "message", "un lien de réinitialisation a été envoyé a votre email"
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