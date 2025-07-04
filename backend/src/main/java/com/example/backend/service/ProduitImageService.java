package com.example.backend.service;

import com.example.backend.model.Image;
import com.example.backend.model.ImageData;
import com.example.backend.model.Produit;
import com.example.backend.repository.ImageDataRepository;
import com.example.backend.repository.ImageRepository;
import com.example.backend.repository.ProduitRepository;
import com.example.backend.util.ImageUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ProduitImageService {

    @Autowired
    private ImageDataRepository imageDataRepository;
    
    @Autowired
    private ProduitRepository produitRepository;
    
    @Autowired
    private ImageRepository imageRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public String uploadProductImage(Long produitId, MultipartFile file) throws IOException {
        Optional<Produit> produitOpt = produitRepository.findById(produitId);
        if (!produitOpt.isPresent()) {
            throw new RuntimeException("Product not found with id: " + produitId);
        }
        Produit produit = produitOpt.get();
        String uniqueImageName = "product_" + produitId + "_image_" + System.currentTimeMillis();

        List<Image> existingImages = imageRepository.findByProduit(produit);
        for (Image existingImage : existingImages) {
            Optional<ImageData> imageDataOpt = imageDataRepository.findByName(existingImage.getName());
            if (imageDataOpt.isPresent()) {
                imageDataRepository.delete(imageDataOpt.get());
            }
            imageRepository.delete(existingImage);
        }

        entityManager.flush();
        ImageData imageData = ImageData.builder()
                .name(uniqueImageName)
                .type(file.getContentType())
                .imageData(ImageUtil.compressImage(file.getBytes()))
                .build();
        imageDataRepository.save(imageData);

        Image image = new Image();
        image.setName(uniqueImageName);
        image.setUrl("/image/" + uniqueImageName);
        image.setProduit(produit);
        imageRepository.save(image);

        produit.setImageUrl(image.getUrl());
        produitRepository.save(produit);
        
        return "Image uploaded successfully for product: " + produit.getNom();
    }
    
    @Transactional
    public byte[] getProductImage(Long produitId) {
        Optional<Produit> produitOpt = produitRepository.findById(produitId);
        
        if (!produitOpt.isPresent()) {
            throw new RuntimeException("Product not found with id: " + produitId);
        }
        
        Produit produit = produitOpt.get();
        
        if (produit.getImageUrl() == null || produit.getImageUrl().isEmpty()) {
            throw new RuntimeException("No image found for product: " + produit.getNom());
        }

        String imageName = produit.getImageUrl().substring(produit.getImageUrl().lastIndexOf('/') + 1);
        
        Optional<ImageData> dbImage = imageDataRepository.findByName(imageName);
        if (!dbImage.isPresent()) {
            throw new RuntimeException("Image not found: " + imageName);
        }
        
        return ImageUtil.decompressImage(dbImage.get().getImageData());
    }
}