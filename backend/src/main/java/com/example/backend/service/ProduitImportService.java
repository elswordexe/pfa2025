package com.example.backend.service;

import com.example.backend.model.Category;
import com.example.backend.model.Produit;
import com.example.backend.model.SubCategory;
import com.example.backend.repository.CategorieRepository;
import com.example.backend.repository.ProduitRepository;
import com.example.backend.repository.SubCategoryRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProduitImportService {
    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private CategorieRepository categoryRepository;
    @Autowired
    private SubCategoryRepository subCategoryRepository;
    public List<Produit> importFromExcel(MultipartFile file) throws IOException {
        List<Produit> produits = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                
                Produit produit = new Produit();
                produit.setReference(getStringCellValue(row.getCell(0)));
                produit.setNom(getStringCellValue(row.getCell(1)));
                produit.setDescription(getStringCellValue(row.getCell(2)));
                produit.setQuantitetheo(getNumericCellValue(row.getCell(3)));
                
                produits.add(produit);
            }
        }
        
        return produitRepository.saveAll(produits);
    }

    public List<Produit> importFromCsv(MultipartFile file) throws IOException {
        List<Produit> produits = new ArrayList<>();
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            MappingIterator<Map<String, String>> it = mapper
                .readerFor(Map.class)
                .with(schema)
                .readValues(reader);

            while (it.hasNext()) {
                Map<String, String> row = it.next();
                Produit produit = new Produit();
                produit.setReference(row.get("reference"));
                produit.setNom(row.get("nom"));
                produit.setDescription(row.get("description"));
                produit.setQuantitetheo(Integer.parseInt(row.get("quantitetheo")));
                   String categoryName = row.get("category.name");
                if (categoryName != null && !categoryName.isEmpty()) {
                    Optional<Category> existingCategory = categoryRepository.findByName(categoryName);
                    Category category;
                    if (existingCategory.isPresent()) {
                        category = existingCategory.get();
                    } else {
                        category = new Category();
                        category.setName(categoryName);
                        category = categoryRepository.save(category);
                    }
                    produit.setCategory(category);
                }
                
                produits.add(produit);
            }
        }

        return produitRepository.saveAll(produits);
    }

    public List<Produit> importFromJson(MultipartFile file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        List<Produit> importedProduits = mapper.readValue(
            file.getInputStream(),
            mapper.getTypeFactory().constructCollectionType(List.class, Produit.class)
        );
        
        List<Produit> produits = new ArrayList<>();
        Map<String, Category> categoryCache = new HashMap<>();
        Map<String, SubCategory> subCategoryCache = new HashMap<>();
        
        for (Produit produit : importedProduits) {
            // Handle category
            if (produit.getCategory() != null && produit.getCategory().getName() != null) {
                String categoryName = produit.getCategory().getName();
                Category category = categoryCache.computeIfAbsent(categoryName, name -> {
                    return categoryRepository.findByName(name)
                        .orElseGet(() -> {
                            Category newCategory = new Category();
                            newCategory.setName(name);
                            return categoryRepository.save(newCategory);
                        });
                });

                if (produit.getSubCategory() != null && produit.getSubCategory().getName() != null) {
                    String subCategoryName = produit.getSubCategory().getName();
                    String cacheKey = categoryName + ":" + subCategoryName;
                    
                    SubCategory subCategory = subCategoryCache.computeIfAbsent(cacheKey, k -> {
                        return subCategoryRepository.findByNameAndCategoryId(subCategoryName, category.getId())
                            .orElseGet(() -> {
                                SubCategory newSubCategory = new SubCategory();
                                newSubCategory.setName(subCategoryName);
                                newSubCategory.setCategory(category);
                                return subCategoryRepository.save(newSubCategory);
                            });
                    });
                    
                    produit.setSubCategory(subCategory);
                }
                
                produit.setCategory(category);
            }
            
            produits.add(produit);
        }
        
        return produitRepository.saveAll(produits);
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            default:
                return "";
        }
    }

    private int getNumericCellValue(Cell cell) {
        if (cell == null) return 0;
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    return 0;
                }
            default:
                return 0;
        }
    }
}