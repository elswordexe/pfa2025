package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
        include = JsonTypeInfo.As.PROPERTY,
        property = "dtype",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AdministrateurClient.class, name = "ADMIN_CLIENT"),
        @JsonSubTypes.Type(value = SuperAdministrateur.class, name = "SUPER_ADMIN"),
        @JsonSubTypes.Type(value = AgentInventaire.class, name = "AGENT_INVENTAIRE"),
        @JsonSubTypes.Type(value = Client.class, name = "Client")
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
    @Column(nullable = false)
    private LocalDateTime datecremod = LocalDateTime.now();
    public Utilisateur() {
        this.role = Role.Utilisateur;
    }

    @Column
    private String resetPasswordToken;

    @Column
    private LocalDateTime resetPasswordTokenExpiryDate;

    @Override
    @JsonIgnore
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

    public LocalDateTime getDatecremod() {
        return datecremod;
    }

    public void setDatecremod(LocalDateTime datecremod) {
        this.datecremod = datecremod;
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

    public String getResetPasswordToken() {
        return resetPasswordToken;
    }

    public void setResetPasswordToken(String resetPasswordToken) {
        this.resetPasswordToken = resetPasswordToken;
    }

    public LocalDateTime getResetPasswordTokenExpiryDate() {
        return resetPasswordTokenExpiryDate;
    }

    public void setResetPasswordTokenExpiryDate(LocalDateTime resetPasswordTokenExpiryDate) {
        this.resetPasswordTokenExpiryDate = resetPasswordTokenExpiryDate;
    }

    public LocalDateTime getDateCreation() {
        return datecremod;
    }

    @Override
    public String getUsername() {
        return (username != null && !username.isBlank()) ? username : email;
    }

}