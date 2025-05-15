package com.example.backend.controller;

import com.example.backend.model.Produit;
import com.example.backend.model.Zone;
import com.example.backend.repository.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class ProduitController {
    @Autowired
    ProduitRepository produitRepository;
    @GetMapping("Produits")
    public Iterable<Produit> getAllProduits(){
        return produitRepository.findAll();
    }
    @GetMapping("Produits/{produitId}")
    public Produit getProduitById(@PathVariable Long produitId){
        return produitRepository.findById(produitId).orElse(null);
    }
    @PostMapping("Produit/register")
    public ResponseEntity<?> registerProduit(@RequestBody Produit produit){
        Produit produit1 = produitRepository.save(produit);
        return ResponseEntity.ok(produit1);
    }
    @DeleteMapping("Produits/{produitId}")
    public ResponseEntity<?> deleteProduit(@PathVariable Long produitId){
        produitRepository.deleteById(produitId);
        return ResponseEntity.ok().build();
    }
    @PutMapping("Produit/{id}")
    public ResponseEntity<?> updateProduit(@PathVariable Long id, @RequestBody Produit produit){
        Produit produit1 = produitRepository.save(produit);
        return ResponseEntity.ok(produit1);
    }
}
