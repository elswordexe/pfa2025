package com.example.backend.controller;

import com.example.backend.model.Category;
import com.example.backend.model.SubCategory;
import com.example.backend.repository.CategorieRepository;
import com.example.backend.repository.SubCategoryRepository;
import com.example.backend.dto.CategoryDTO;
import com.example.backend.dto.SubCategoryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;



@RestController
@RequestMapping("/api/categories")
@CrossOrigin("*")
@Tag(name = "Categories Management", description = "APIs pour gérer les catégories")
public class CategoryController {

    @Autowired
    private CategorieRepository categoryRepository;

    @Autowired
    private SubCategoryRepository subcategoryRepository;

    @Operation(summary = "Récupération des catégories")
    @ApiResponse(responseCode = "200", description = "Liste des catégories",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryDTO.class))))
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        try {
            List<Category> categories = categoryRepository.findAll();
            List<CategoryDTO> dtos = categories.stream()
                .map(category -> {
                    CategoryDTO dto = new CategoryDTO();
                    dto.setId(category.getId());
                    dto.setName(category.getName());
                    dto.setSubCategories(category.getSubCategories().stream()
                        .map(sub -> {
                            SubCategoryDTO subDto = new SubCategoryDTO();
                            subDto.setId(sub.getId());
                            subDto.setName(sub.getName());
                            return subDto;
                        })
                        .collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Création d'une catégorie",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Données de la catégorie à créer",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Category.class),
                            examples = @ExampleObject(
                                    name = "Exemple de catégorie",
                                    value = "{\n  \"name\": \"Mobilier\"\n}"
                            )
                    )
            )
    )
    @ApiResponse(responseCode = "201", description = "Catégorie créée",
            content = @Content(schema = @Schema(implementation = CategoryDTO.class)))
    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(@RequestBody Category category) {
        try {
            Optional<Category> existingCategoryOpt = categoryRepository.findByName(category.getName());
        
            Category savedCategory;
            if (existingCategoryOpt.isPresent()) {
                savedCategory = existingCategoryOpt.get();
                return new ResponseEntity<>(convertToDTO(savedCategory), HttpStatus.OK);
            }

            savedCategory = categoryRepository.save(category);
            return new ResponseEntity<>(convertToDTO(savedCategory), HttpStatus.CREATED);
        
    } catch (Exception e) {
        e.printStackTrace();
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    }

    @Operation(
            summary = "Mise à jour d'une catégorie",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Données de la catégorie à mettre à jour",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Category.class),
                            examples = @ExampleObject(
                                    name = "Exemple de mise à jour de catégorie",
                                    value = "{\n  \"name\": \"Mobilier de bureau\"\n}"
                            )
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "Catégorie mise à jour",
            content = @Content(schema = @Schema(implementation = Category.class)))
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category categoryDetails) {
        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'id " + id));

            category.setName(categoryDetails.getName());
            Category updatedCategory = categoryRepository.save(category);
            return ResponseEntity.ok(updatedCategory);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Suppression d'une catégorie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Catégorie supprimée"),
        @ApiResponse(responseCode = "400", description = "Impossible de supprimer la catégorie")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable: " + id));

            if (!category.getSubCategories().isEmpty()) {
                return new ResponseEntity<>("Impossible de supprimer une catégorie avec des sous-catégories", HttpStatus.BAD_REQUEST);
            }
            if (!category.getProduits().isEmpty()) {
                return new ResponseEntity<>("Impossible de supprimer une catégorie avec des produits", HttpStatus.BAD_REQUEST);
            }

            categoryRepository.delete(category);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Récupérer les sous-catégories")
    @ApiResponse(responseCode = "200", description = "Liste des sous-catégories")
    @GetMapping("/{categoryId}/sous-categories")
    public ResponseEntity<List<SubCategory>> getSubCategories(@PathVariable Long categoryId) {
        try {
            List<SubCategory> subCategories = subcategoryRepository.findByCategoryId(categoryId);
            return ResponseEntity.ok(subCategories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Ajouter une sous-catégorie",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Données de la sous-catégorie à ajouter",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SubCategory.class),
                            examples = @ExampleObject(
                                    name = "Exemple de sous-catégorie",
                                    value = "{\n  \"name\": \"Chaises\"\n}"
                            )
                    )
            )
    )
    @ApiResponse(responseCode = "201", description = "Sous-catégorie créée")
    @PostMapping("/{categoryId}/sous-categories")
    public ResponseEntity<SubCategory> addSubCategory(
            @PathVariable Long categoryId,
            @RequestBody SubCategory subCategory) {
        try {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));

            subCategory.setCategory(category);
            SubCategory savedSubCategory = subcategoryRepository.save(subCategory);
            
            return new ResponseEntity<>(savedSubCategory, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Supprimer une sous-catégorie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sous-catégorie supprimée"),
        @ApiResponse(responseCode = "400", description = "Impossible de supprimer la sous-catégorie")
    })
    @DeleteMapping("/{categoryId}/sous-categories/{subCategoryId}")
    public ResponseEntity<?> deleteSubCategory(
            @PathVariable Long categoryId,
            @PathVariable Long subCategoryId) {
        try {
            SubCategory subCategory = subcategoryRepository.findById(subCategoryId)
                    .orElseThrow(() -> new RuntimeException("Sous-catégorie non trouvée"));

            if (!subCategory.getCategory().getId().equals(categoryId)) {
                return ResponseEntity.badRequest()
                    .body("Cette sous-catégorie n'appartient pas à la catégorie spécifiée");
            }

            if (!subCategory.getProduits().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("Impossible de supprimer une sous-catégorie contenant des produits");
            }

            subcategoryRepository.delete(subCategory);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
private CategoryDTO convertToDTO(Category category) {
    if (category == null) {
        return null;
    }
    
    List<SubCategoryDTO> subCategoryDTOs = category.getSubCategories() == null ? 
        List.of() :
        category.getSubCategories().stream()
            .map(sub -> new SubCategoryDTO(
                sub.getId(),
                sub.getName())
            )
            .collect(Collectors.toList());
            
    return new CategoryDTO(
        category.getId(),
        category.getName(),
        subCategoryDTOs
    );
}
}