package com.example.backend.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportDTO {
    private String nom;
    private String description;
    private String codeBarre;
    private String reference;
    private Double prix;
    private String unite;
    private Integer quantitetheo;
    private CategoryImportDTO category;
    private CategoryImportDTO subCategory;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryImportDTO {
        private String name;
    }
}