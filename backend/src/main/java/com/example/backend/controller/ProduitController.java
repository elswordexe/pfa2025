package com.example.backend.controller;
import com.example.backend.dto.ProduitDTO;
import com.example.backend.dto.ProductImportDTO;
import com.example.backend.model.*;
import com.example.backend.repository.CategorieRepository;
import com.example.backend.repository.ProduitRepository;
import com.example.backend.repository.SubCategoryRepository;
import com.example.backend.repository.ZoneRepository;
import com.example.backend.service.ProduitImageService;
import com.example.backend.service.ProduitImportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.ArraySchema;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/produits")
@CrossOrigin(origins = "*")
@Tag(name = "controller des Produits", description = "APIs des Produits")
public class ProduitController {

    private final ProduitRepository produitRepository;
    @Autowired
    private ProduitImageService produitImageService;
    @Autowired
    ZoneRepository zoneRepository;
    @Autowired
    ProduitImportService produitImportService;
    @Autowired
    SubCategoryRepository subCategoryRepository;
    @Autowired
    public ProduitController(ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
    }
    @Autowired
    CategorieRepository categoryRepository;

    @Operation(summary = "Retourner les infos d'un seul produit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produit listé avec succès",
                    content = @Content(schema = @Schema(implementation = ProduitDTO.class))),
            @ApiResponse(responseCode = "404", description = "Produit non trouvé")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProduitDTO> getProductById(@PathVariable Long id) {
        try {
            Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
            
            return ResponseEntity.ok(convertToDTO(produit));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }

    @Operation(
            summary = "Ajout d'un nouveau produit",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Produit à ajouter",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Produit.class),
                            examples = @ExampleObject(
                                    name = "Exemple d'ajout de produit",
                                    value = "{\n  \"nom\": \"Chaise\",\n  \"prix\": 99.99,\n  \"unite\": \"piece\",\n  \"description\": \"Chaise ergonomique\",\n  \"reference\": \"CH-12345\",\n  \"CodeBarre\": \"1234567890123\",\n  \"imageUrl\": \"https://example.com/chaise.jpg\",\n  \"category\": { \"id\": 1 },\n  \"zones\": [ { \"id\": 1 }, { \"id\": 2 } ]\n}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produit ajouté avec succès",
                    content = @Content(schema = @Schema(implementation = Produit.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> registerProduit(@RequestBody Map<String, Object> requestMap) {
        try {
            Produit produit = new Produit();

            if (requestMap.containsKey("nom")) produit.setNom((String) requestMap.get("nom"));
            if (requestMap.containsKey("prix")) {
                Object prixObj = requestMap.get("prix");
                if (prixObj instanceof Number) {
                    produit.setPrix(((Number) prixObj).doubleValue());
                } else if (prixObj instanceof String) {
                    produit.setPrix(Double.parseDouble((String) prixObj));
                }
            }
            if (requestMap.containsKey("unite")) produit.setUnite((String) requestMap.get("unite"));
            if (requestMap.containsKey("description")) produit.setDescription((String) requestMap.get("description"));
            if (requestMap.containsKey("reference")) produit.setReference((String) requestMap.get("reference"));
            if (requestMap.containsKey("CodeBarre")) produit.setCodeBarre((String) requestMap.get("CodeBarre"));
            if (requestMap.containsKey("imageUrl")) produit.setImageUrl((String) requestMap.get("imageUrl"));
            if (requestMap.containsKey("quantitetheo")) {
                Object qteObj = requestMap.get("quantitetheo");
                if (qteObj instanceof Number) {
                    produit.setQuantitetheo(((Number) qteObj).intValue());
                } else if (qteObj instanceof String) {
                    produit.setQuantitetheo(Integer.parseInt((String) qteObj));
                }
            }
            if (requestMap.containsKey("category") && requestMap.get("category") != null) {
                Map<String, Object> categoryMap = (Map<String, Object>) requestMap.get("category");
                if (categoryMap.containsKey("id")) {
                    Category category = new Category();
                    category.setId(Long.valueOf(categoryMap.get("id").toString()));
                    produit.setCategory(category);
                }
            }
            if (requestMap.containsKey("subCategory") && requestMap.get("subCategory") != null) {
                Map<String, Object> subCategoryMap = (Map<String, Object>) requestMap.get("subCategory");
                if (subCategoryMap.containsKey("id")) {
                    SubCategory subCategory = new SubCategory();
                    subCategory.setId(Long.valueOf(subCategoryMap.get("id").toString()));
                    produit.setSubCategory(subCategory);
                }
            }
            produit.setZones(new ArrayList<>());
            Produit savedProduit = produitRepository.save(produit);
            List<Long> zoneIds = new ArrayList<>();
            if (requestMap.containsKey("zone") && requestMap.get("zone") != null) {
                Map<String, Object> zoneMap = (Map<String, Object>) requestMap.get("zone");
                if (zoneMap.containsKey("id")) {
                    zoneIds.add(Long.valueOf(zoneMap.get("id").toString()));
                }
            }
            if (requestMap.containsKey("zones") && requestMap.get("zones") != null) {
                List<Map<String, Object>> zonesList = (List<Map<String, Object>>) requestMap.get("zones");
                for (Map<String, Object> zoneMap : zonesList) {
                    if (zoneMap.containsKey("id")) {
                        zoneIds.add(Long.valueOf(zoneMap.get("id").toString()));
                    }
                }
            }

            for (Long zoneId : zoneIds) {
                Optional<Zone> existingZone = zoneRepository.findById(zoneId);
                if (existingZone.isPresent()) {
                    Zone actualZone = existingZone.get();
                    if (actualZone.getProduits() == null) {
                        actualZone.setProduits(new ArrayList<>());
                    }
                    if (!actualZone.getProduits().contains(savedProduit)) {
                        actualZone.getProduits().add(savedProduit);
                    }
                    if (savedProduit.getZones() == null) {
                        savedProduit.setZones(new ArrayList<>());
                    }
                    if (!savedProduit.getZones().contains(actualZone)) {
                        savedProduit.getZones().add(actualZone);
                    }

                    zoneRepository.save(actualZone);
                }
            }
            savedProduit = produitRepository.save(savedProduit);
            return ResponseEntity.ok(savedProduit);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erreur lors de l'ajout du produit: " + e.getMessage());
        }
    }

    @Operation(summary = "Supprimer un produit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produit supprimé avec succès"),
            @ApiResponse(responseCode = "404", description = "Produit non trouvé")
    })
    @DeleteMapping("/{produitId}")
    public ResponseEntity<?> deleteProduit(@PathVariable Long produitId){
        return produitRepository.findById(produitId)
            .map(produit -> {
                produitRepository.delete(produit);
                return ResponseEntity.ok().build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Mettre à jour un produit",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Données du produit à mettre à jour",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Produit.class),
                            examples = @ExampleObject(
                                    name = "Exemple de mise à jour de produit",
                                    value = "{\n  \"nom\": \"Chaise Deluxe\",\n  \"prix\": 129.99,\n  \"unite\": \"piece\",\n  \"description\": \"Chaise ergonomique améliorée\",\n  \"reference\": \"CH-12345\",\n  \"CodeBarre\": \"1234567890123\",\n  \"imageUrl\": \"https://example.com/chaise_deluxe.jpg\",\n  \"category\": { \"id\": 1 },\n  \"zones\": [ { \"id\": 3 } ]\n}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produit mis à jour avec succès"),
            @ApiResponse(responseCode = "404", description = "Produit non trouvé")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduit(@PathVariable Long id, @RequestBody Produit produit){
        if (!produitRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        produit.setId(id);
        Produit produit1 = produitRepository.save(produit);
        return ResponseEntity.ok(produit1);
    }

    @Operation(summary = "Rechercher des produits")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Résultats de recherche",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Produit.class))))
    })
    @GetMapping("/search")
    public ResponseEntity<List<Produit>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {
        List<Produit> results;
        if (keyword != null && !keyword.trim().isEmpty()) {
            results = produitRepository.findByNomContainingOrDescriptionContainingOrCodeBarreContainingOrReferenceContaining(
                    keyword, keyword, keyword, keyword);
        } else if (categoryId != null) {
            results = produitRepository.findByCategoryId(categoryId);
        } else {
            results = produitRepository.findAll();
        }
        if (minPrice != null || maxPrice != null) {
            double minPriceValue = minPrice != null ? minPrice : 0;
            double maxPriceValue = maxPrice != null ? maxPrice : Double.MAX_VALUE;

            results = results.stream()
                    .filter(p -> p.getPrix() >= minPriceValue && p.getPrix() <= maxPriceValue)
                    .toList();
        }
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Obtenir les unités de mesure disponibles")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des unités de mesure"),
    })
    @GetMapping("/unites")
    public ResponseEntity<List<String>> getUnitesOfMeasure() {
        List<String> unites = produitRepository.findDistinctUnites();
        return ResponseEntity.ok(unites);
    }
    @Operation(summary = "afficher par code barecode scannée ")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "lister les données du produit scannée avec succes"),@ApiResponse(responseCode = "404",description = "erreur survenue produit non trouvee ou bien veuillez scanner de nouveau")})
    @GetMapping("/{barreCode}")
    public Produit getByBareCode(@PathVariable String barreCode){
        return produitRepository.findByCodeBarre(barreCode).orElse(null);
    }
    @Operation(summary ="Obetenir nombre des produits")
    @ApiResponses(value={ @ApiResponse(responseCode = "200", description = "lister nombre des produits"),})
    @GetMapping("/count")
    public Long getProduitCount(){
       return  produitRepository.count();
    }
    @Operation(summary = "Obtenir des produits par zone")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Liste des produits par zone"), })
    @GetMapping("/byZone/{id}")
    public List<Produit> getProduitsByZone(@PathVariable Long id) {
    Optional<Zone> zone = zoneRepository.findById(id);
    return zone.map(Zone::getProduits).orElse(List.of());
    }
    @Operation(summary = "insertion d' images des produits")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "insértion avec succès"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de l'insertion d'images")
    })
    @PostMapping("/{produitId}/image")
    public ResponseEntity<?> uploadProductImage(
            @PathVariable("produitId") Long produitId,
            @RequestParam("image") MultipartFile file) throws IOException {

        String response = produitImageService.uploadProductImage(produitId, file);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }
    @Operation(summary = "Obtenir les images des produits")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "récupération avec succès"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des données")
    })
    @GetMapping("/{produitId}/image")
    public ResponseEntity<?> getProductImage(@PathVariable("produitId") Long produitId) {
        byte[] image = produitImageService.getProductImage(produitId);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.IMAGE_JPEG)
                .body(image);
    }
    @Operation(summary = "Obtenir les noms, dates et images des produits")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès"),
        @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des données")
    })
    @GetMapping("/names-dates")
    public ResponseEntity<List<Map<String, Object>>> getProduitsNameAndDate(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    try {
        Pageable pageable = PageRequest.of(page, size);
        Page<Produit> produitPage = produitRepository.findAll(pageable);

        List<Map<String, Object>> produitsData = produitPage.getContent().stream()
                .map(produit -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", produit.getId());
                    data.put("name", produit.getNom());
                    data.put("date", produit.getDatecremod());
                    data.put("imageUrl", produit.getImageUrl());
                    if (produit.getId() != null && produit.getImageUrl() != null && !produit.getImageUrl().isEmpty()) {
                        try {
                            byte[] imageData = produitImageService.getProductImage(produit.getId());
                            String base64Image = Base64.getEncoder().encodeToString(imageData);
                            data.put("imageData", base64Image);
                        } catch (Exception e) {
                            System.err.println("Could not retrieve image for product " + produit.getId() + ": " + e.getMessage());
                        }
                    }

                    return data;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(produitsData);
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(List.of());
    }
}
@Operation(summary = "importer les donnes des produits sous format Excel")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "importé avec succès"),
            @ApiResponse(responseCode = "404", description = "importation échoué")
    })
@PostMapping("/import/excel")
public ResponseEntity<?> importFromExcel(@RequestParam("file") MultipartFile file) {
    try {
        if (!file.getOriginalFilename().endsWith(".xlsx")) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Le fichier doit être au format .xlsx"));
        }

        List<Produit> imported = produitImportService.importFromExcel(file);
        return ResponseEntity.ok(Map.of(
            "message", "Import réussi",
            "count", imported.size(),
            "produits", imported
        ));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", "Erreur lors de l'import: " + e.getMessage()));
    }
}
    @Operation(summary = "importer les donnes des produits sous format CSV")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "importé avec succès"),
            @ApiResponse(responseCode = "404", description = "importation échoué")
    })
@PostMapping("/import/csv")
public ResponseEntity<?> importFromCsv(@RequestParam("file") MultipartFile file) {
    try {
        if (!file.getOriginalFilename().endsWith(".csv")) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Le fichier doit être au format .csv"));
        }

        List<Produit> imported = produitImportService.importFromCsv(file);
        return ResponseEntity.ok(Map.of(
            "message", "Import réussi",
            "count", imported.size(),
            "produits", imported
        ));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", "Erreur lors de l'import: " + e.getMessage()));
    }
}

    @Operation(
            summary = "importer les donnes des produits sous format JSON",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Liste des produits à importer",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProductImportDTO.class),
                            examples = @ExampleObject(
                                    name = "Exemple d'import JSON",
                                    value = "[{\n  \"nom\": \"Table\",\n  \"prix\": 149.99,\n  \"unite\": \"piece\",\n  \"description\": \"Table en bois\",\n  \"reference\": \"TB-001\",\n  \"codeBarre\": \"9876543210987\",\n  \"quantitetheo\": 100,\n  \"category\": { \"name\": \"Mobilier\" },\n  \"subCategory\": { \"name\": \"Tables\" }\n}]"
                            )
                    )
            )
    )
@PostMapping("/import/json")
public ResponseEntity<?> importProducts(@RequestBody List<ProductImportDTO> productsDTO) {
    try {
        List<Produit> savedProducts = new ArrayList<>();
        
        for (ProductImportDTO dto : productsDTO) {
            Produit produit = new Produit();
            produit.setNom(dto.getNom());
            produit.setDescription(dto.getDescription());
            produit.setCodeBarre(dto.getCodeBarre());
            produit.setReference(dto.getReference());
            produit.setPrix(dto.getPrix());
            produit.setUnite(dto.getUnite());
            produit.setQuantitetheo(dto.getQuantitetheo());

            if (dto.getCategory() != null) {
                Category category = categoryRepository.findByName(dto.getCategory().getName())
                    .orElseGet(() -> {
                        Category newCategory = new Category();
                        newCategory.setName(dto.getCategory().getName());
                        return categoryRepository.save(newCategory);
                    });
                produit.setCategory(category);
            }
            if (dto.getSubCategory() != null) {
                SubCategory subCategory = subCategoryRepository.findByName(dto.getSubCategory().getName())
                    .orElseGet(() -> {
                        SubCategory newSubCategory = new SubCategory();
                        newSubCategory.setName(dto.getSubCategory().getName());
                        newSubCategory.setCategory(produit.getCategory());
                        return subCategoryRepository.save(newSubCategory);
                    });
                produit.setSubCategory(subCategory);
            }
            
            savedProducts.add(produitRepository.save(produit));
        }
        
        return ResponseEntity.ok(savedProducts);
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Error importing products: " + e.getMessage());
    }
}

    @Operation(summary = "Retourner toutes les  produit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produits listés",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProduitDTO.class)))),
            @ApiResponse(responseCode = "404", description = "erreur aucun produit")
    })
@GetMapping
public ResponseEntity<List<ProduitDTO>> getAllProduits(
    @RequestParam(required = false) Long categoryId,
    @RequestParam(required = false) Long subCategoryId
) {
    try {
        List<Produit> produits;
        if (subCategoryId != null) {
            produits = produitRepository.findBySubCategoryId(subCategoryId);
        } else if (categoryId != null) {
            produits = produitRepository.findByCategoryId(categoryId);
        } else {
            produits = produitRepository.findAll();
        }

        List<ProduitDTO> dtos = produits.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

    private ProduitDTO convertToDTO(Produit produit) {
        ProduitDTO dto = new ProduitDTO();
        dto.setId(produit.getId());
        dto.setNom(produit.getNom());
        dto.setDescription(produit.getDescription());
        dto.setCodeBarre(produit.getCodeBarre());
        dto.setReference(produit.getReference());
        dto.setPrix(produit.getPrix());
        dto.setUnite(produit.getUnite());
        dto.setQuantitetheo(produit.getQuantitetheo());
        dto.setDatecremod(produit.getDatecremod());
        dto.setImageUrl(produit.getImageUrl());
        // Add base64 image data if available
        if (produit.getId() != null && produit.getImageUrl() != null && !produit.getImageUrl().isEmpty()) {
            try {
                byte[] imageData = produitImageService.getProductImage(produit.getId());
                String base64Image = java.util.Base64.getEncoder().encodeToString(imageData);
                dto.setImageData(base64Image);
            } catch (Exception e) {
                // Optionally log error
            }
        }

        if (produit.getCategory() != null) {
            ProduitDTO.CategoryDTO categoryDTO = new ProduitDTO.CategoryDTO();
            categoryDTO.setId(produit.getCategory().getId());
            categoryDTO.setName(produit.getCategory().getName());
            dto.setCategory(categoryDTO);
        }

        if (produit.getSubCategory() != null) {
            ProduitDTO.SubCategoryDTO subCategoryDTO = new ProduitDTO.SubCategoryDTO();
            subCategoryDTO.setId(produit.getSubCategory().getId());
            subCategoryDTO.setName(produit.getSubCategory().getName());
            dto.setSubCategory(subCategoryDTO);
        }

        if (produit.getZones() != null) {
            produit.getZones().forEach(z -> {
                ProduitDTO.ZoneDTO zDto = new ProduitDTO.ZoneDTO();
                zDto.setId(z.getId());
                zDto.setName(z.getName());
                dto.getZones().add(zDto);
            });
        }

        return dto;
    }

   @Operation(
           summary = "Mettre à jour la quantité et le statut d'un produit dans une zone spécifique",
           requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                   description = "Quantité théorique et statut du produit",
                   required = true,
                   content = @Content(
                           mediaType = "application/json",
                           examples = @ExampleObject(
                                   name = "Exemple de mise à jour quantité/statut",
                                   value = "{\n  \"quantiteTheorique\": 50,\n  \"status\": \"EN_STOCK\"\n}"
                           )
                   )
           )
   )
    @ApiResponse(responseCode = "200", description = "Quantité et statut mis à jour avec succès")
    @PutMapping("/{produitId}/zones/{zoneId}/updateQuantite")
    public ResponseEntity<?> updateProduitQuantiteInZone(
            @PathVariable Long produitId,
            @PathVariable Long zoneId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            Integer newQuantity = null;
            Object quantityObj = requestBody.get("quantiteTheorique");
            if (quantityObj != null) {
                if (quantityObj instanceof Integer) {
                    newQuantity = (Integer) quantityObj;
                } else if (quantityObj instanceof Number) {
                    newQuantity = ((Number) quantityObj).intValue();
                } else if (quantityObj instanceof String) {
                    try {
                        newQuantity = Integer.parseInt((String) quantityObj);
                    } catch (NumberFormatException e) {
                        return ResponseEntity.badRequest()
                            .body(Map.of("error", "La quantité théorique doit être un nombre valide"));
                    }
                }
            }
            String status = requestBody.get("status") != null ? String.valueOf(requestBody.get("status")) : null;
            System.out.println("Received request body: " + requestBody);
            System.out.println("Request body type: " + (requestBody.get("quantiteTheorique") != null ? requestBody.get("quantiteTheorique").getClass().getName() : "null"));
            System.out.println("Converted quantity: " + newQuantity);
            System.out.println("Raw quantity value: " + requestBody.get("quantiteTheorique"));
            System.out.println("Status: " + status);

            if (requestBody.get("quantiteTheorique") == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "La quantité théorique est requise",
                    "details", "Le champ quantiteTheorique est manquant dans la requête"
                ));
            }

            if (newQuantity == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "La quantité théorique est invalide",
                    "details", "Impossible de convertir la valeur en nombre"
                ));
            }

            if (newQuantity < 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "La quantité théorique ne peut pas être négative",
                    "details", "La valeur doit être supérieure ou égale à 0"
                ));
            }

            Optional<Produit> produitOpt = produitRepository.findById(produitId);
            Optional<Zone> zoneOpt = zoneRepository.findById(zoneId);

            if (produitOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Produit non trouvé avec l'id: " + produitId));
            }

            if (zoneOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Zone non trouvée avec l'id: " + zoneId));
            }

            Produit produit = produitOpt.get();
            Zone zone = zoneOpt.get();

            // Vérifier que le produit appartient à la zone
            if (!zone.getProduits().contains(produit)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Le produit n'appartient pas à cette zone"));
            }

            // Rechercher le lien ZoneProduit correspondant
            ZoneProduit targetZp = null;
            for (ZoneProduit zp : produit.getZoneProduits()) {
                if (zp.getZone().getId().equals(zoneId)) {
                    targetZp = zp;
                    break;
                }
            }
            if (targetZp == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "ZoneProduit non trouvé pour ce produit et cette zone"));
            }
            targetZp.setQuantiteTheorique(newQuantity);

            if ("VERIFIE".equalsIgnoreCase(status)) {
                targetZp.setVerified(true);
            }

            int totalQuantite = produit.getZoneProduits().stream()
                    .map(ZoneProduit::getQuantiteTheorique)
                    .filter(q -> q != null)
                    .mapToInt(Integer::intValue)
                    .sum();

            produit.setQuantitetheo(totalQuantite);

            produitRepository.save(produit);

            return ResponseEntity.ok(Map.of(
                "message", "Mise à jour réussie",
                "quantiteTheoriqueZone", newQuantity,
                "quantiteTheoriqueProduit", totalQuantite
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}