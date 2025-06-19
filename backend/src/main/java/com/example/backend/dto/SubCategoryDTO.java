package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubCategoryDTO {
    private Long id;
    private String name;
    private Long categoryId;

    public SubCategoryDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}