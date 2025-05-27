package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;


@Entity
@Table(name = "utilisateurs")
@Getter
@Setter
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)@DiscriminatorColumn(name = "dtype")
@DiscriminatorValue("Utilisateur")
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "dtype",
        defaultImpl = Utilisateur.class
)

@JsonSubTypes({
        @JsonSubTypes.Type(value = AdministrateurClient.class, name = "AdministrateurClient"),
        @JsonSubTypes.Type(value = SuperAdministrateur.class, name = "SuperAdminisateur"),
        @JsonSubTypes.Type(value = AgentInventaire.class, name = "AgentInventaire")
})

public class Utilisateur implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String telephone;
    @Enumerated(EnumType.STRING)
    private Role role;
    private LocalDateTime datecremod;
    public Utilisateur() {
        this.role = Role.Utilisateur;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + Role.Utilisateur.name()));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Role getRole() {
        return role != null ? role : Role.Utilisateur;
    }

    public Utilisateur(String nom, String prenom, String email) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.role = Role.Utilisateur;
    }

    public Utilisateur(String nom, String prenom, String email, String password) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = Role.Utilisateur;
    }
}