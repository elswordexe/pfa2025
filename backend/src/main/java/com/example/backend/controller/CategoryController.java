package com.example.backend.controller;

import com.example.backend.model.Category;
import com.example.backend.repository.CategorieRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin("*")
@Tag(name="categories" ,description="Api des categories")
public class CategoryController {

    @Autowired
    private CategorieRepository categorieRepository;

    @Operation(summary ="recuperation de la catgeorie", description = "gayrouha dba blawlid dial lbatali")
    @ApiResponse(responseCode = "200", description = "categories list")
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        List<Category> rootCategories = categorieRepository.findAll().stream()
                .filter(category -> category.getParentCategory() == null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rootCategories);
    }
    @Operation(summary="creation ert ertet" ,description="Api des categories")
    @ApiResponse(responseCode = "200", description = "categories list")
    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        try {
            if (category.getParentCategory() != null && category.getParentCategory().getId() != null) {
                Category parentCategory = categorieRepository.findById(category.getParentCategory().getId())
                        .orElseThrow(() -> new RuntimeException("category Parent non trouve"));
                category.setParentCategory(parentCategory);
            }
            Category savedCategory = categorieRepository.save(category);
            return new ResponseEntity<>(savedCategory, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }}
    @Operation(summary ="update de category")
    @ApiResponse( responseCode= "200", description = "updatelist")
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category categoryDetails) {
        try {
            Category category = categorieRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Category pas trouvavec id " + id));

            category.setName(categoryDetails.getName());
            
            if (categoryDetails.getParentCategory() != null && categoryDetails.getParentCategory().getId() != null) {
                Category parentCategory = categorieRepository.findById(categoryDetails.getParentCategory().getId())
                        .orElseThrow(() -> new RuntimeException("category Parent non trouve"));
                category.setParentCategory(parentCategory);
            } else {
                category.setParentCategory(null);
            }

            Category updatedCategory = categorieRepository.save(category);
            return ResponseEntity.ok(updatedCategory);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @Operation(summary = "suppression de la catgeorie", description = "gayrouha dba blawlid dial lbatali")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur enregistré avec succès"),
            @ApiResponse(responseCode = "400", description = "Données d'utilisateur invalides")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            Category category = categorieRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Categort introuvable: " + id));

            if (!category.getSubCategories().isEmpty()) {
                return new ResponseEntity<>("impo supprimer category si il a des sub", HttpStatus.BAD_REQUEST);
            }
            if (!category.getProduits().isEmpty()) {
                return new ResponseEntity<>("impo supprimer category si il a des prod", HttpStatus.BAD_REQUEST);
            }

            categorieRepository.delete(category);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}