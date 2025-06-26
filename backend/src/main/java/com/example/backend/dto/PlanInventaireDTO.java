package com.example.backend.dto;

import com.example.backend.model.Category;
import com.example.backend.model.SubCategory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class PlanInventaireDTO {
    private Long id;
    private String nom;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String type;
    private String statut;
    private String recurrence;
    private Boolean inclusTousProduits;
    private List<ZoneDTO> zones = new ArrayList<>();
    private List<ProduitDTO> produits = new ArrayList<>();
    private LocalDateTime dateCreation;
    private UtilisateurDTO createur;
    private List<AssignationAgentDTO> assignations = new ArrayList<>();
    @Data
    @NoArgsConstructor
    public static class ZoneDTO {
        private String name;
        private Long id;
        private List<ZoneProduitDTO> zoneProduits = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class ProduitDTO {
        private Long id;
        private CategoryDTO categoryDTO;
        private SubCategoryDTO subCategoryDTO;
    }

    @Data
    @NoArgsConstructor
    public static class CategoryDTO {
        private Long id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    public static class SubCategoryDTO {
        private Long id;
        private String name;
        private Long categoryId;
    }

    @Data
    @NoArgsConstructor
    public static class AssignationAgentDTO {
        private Long id;
        private ZoneDTO zone;
        private AgentInventaireDTO agent;
        private LocalDateTime dateAssignation;
    }

    @Data
    @NoArgsConstructor
    public static class AgentInventaireDTO {
        private Long id;
        private String nom;
        private String prenom;
        private String firstName;
        private String lastName;
        private String email;
        private String role;
    }

    @Data
    @NoArgsConstructor
    public static class UtilisateurDTO {
        private Long id;
        private String nom;
        private String prenom;
        private String email;
        private String role;
        private LocalDateTime dateCreation;
        private LocalDateTime dateModification;
    }

    @Data
    @NoArgsConstructor
    public static class ZoneProduitDTO {
        private Long id;
        private Integer quantitetheo;
        private ZoneProduitIdDTO zoneProduitId;
        private ProduitDTO produit;
    }

    @Data
    @NoArgsConstructor
    public static class ZoneProduitIdDTO {
        private Long zoneId;
        private Long produitId;
    }
}


