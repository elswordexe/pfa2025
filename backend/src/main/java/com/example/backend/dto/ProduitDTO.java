package com.example.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProduitDTO {
    private Long id;
    private String codeBarre;
    private String reference;
    private String nom;
    private String description;
    private Double prix;
    private String unite;
    private LocalDateTime datecremod;
    private String imageUrl;
    private String imageData;
    private Integer quantitetheo;
    private SubCategoryDTO subCategoryDTO;
    private CategoryDTO category;
    private SubCategoryDTO subCategory;
    private java.util.List<ZoneDTO> zones = new java.util.ArrayList<>();

    @Data
    public static class CategoryDTO {
        private Long id;
        private String name;
    }
    @Data
    public static class SubCategoryDTO {
        private Long id;
        private String name;
        private Long categoryId;
        private CategoryDTO category;
        private SubCategoryDTO subCategory;
    }

    @Data
    public static class ZoneDTO {
        private Long id;
        private String name;
    }
}