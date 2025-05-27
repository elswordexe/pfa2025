package com.example.backend.repository;

import com.example.backend.model.Image;
import com.example.backend.model.Produit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByProduit(Produit produit);
}