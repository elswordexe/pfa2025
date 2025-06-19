package com.example.backend.dto;

import com.example.backend.model.CheckupType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CheckupDTO {
    private Long id;
    
    @NotNull(message = "Type de checkup est requis")
    private CheckupType type;
    
    @NotNull(message = "Agent est requis")
    @Valid
    private AgentDTO agent;
    
    @NotNull(message = "Plan est requis")
    @Valid
    private PlanDTO plan;
    
    @Valid
    private List<CheckupDetailDTO> details = new ArrayList<>();
    
    private LocalDateTime dateCheck;
    private boolean valide;
    private boolean demandeRecomptage;
    
    @Size(max = 500, message = "La justification ne peut pas dépasser 500 caractères")
    private String justificationRecomptage;

    @Data
    public static class AgentDTO {
        @NotNull(message = "ID de l'agent est requis")
        private Long id;
        private String nom;
        private String prenom;
        private String email;
    }

    @Data
    public static class PlanDTO {
        @NotNull(message = "ID du plan est requis")
        private Long id;
        private String nom;
        private LocalDateTime dateDebut;
        private LocalDateTime dateFin;
    }

    @Data
    public static class CheckupDetailDTO {
        private Long id;
        
        @NotNull(message = "Produit est requis")
        @Valid
        private ProduitDTO produit;
        
        private Integer scannedQuantity;
        private Integer manualQuantity;
    }

    @Data
    public static class ProduitDTO {
        @NotNull(message = "ID du produit est requis")
        private Long id;
        private String codeBarre;
        private String reference;
        private String nom;
        private String description;
        private Integer quantitetheo;
        
        @Data
        public static class CategoryDTO {
            private Long id;
            private String name;
        }
        
        @Data
        public static class SubCategoryDTO {
            private Long id;
            private String name;
        }
        
        private CategoryDTO category;
        private SubCategoryDTO subCategory;
    }
}