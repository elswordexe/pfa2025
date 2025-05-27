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
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.Optional;

@Service
public class ProduitImageService {

    @Autowired
    private ImageDataRepository imageDataRepository;
    
    @Autowired
    private ProduitRepository produitRepository;
    
    @Autowired
    private ImageRepository imageRepository;

    @Transactional
    public String uploadProductImage(Long produitId, MultipartFile file) throws IOException {
        Optional<Produit> produitOpt = produitRepository.findById(produitId);
        
        if (!produitOpt.isPresent()) {
            throw new RuntimeException("Product not found with id: " + produitId);
        }
        
        Produit produit = produitOpt.get();
        ImageData imageData = ImageData.builder()
                .name(file.getOriginalFilename())
                .type(file.getContentType())
                .imageData(ImageUtil.compressImage(file.getBytes()))
                .build();
        
        imageData = imageDataRepository.save(imageData);

        Image image = new Image();
        image.setName(file.getOriginalFilename());
        image.setUrl("/image/" + imageData.getName());
        image.setProduit(produit);
        
        imageRepository.save(image);

        if (produit.getImageUrl() == null || produit.getImageUrl().isEmpty()) {
            produit.setImageUrl("/image/" + imageData.getName());
            produitRepository.save(produit);
        }

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