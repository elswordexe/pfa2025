package com.example.backend.controller;

import com.example.backend.model.Utilisateur;
import com.example.backend.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class UtilisateurController {
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @GetMapping("users")
   public Iterable<Utilisateur> getAllUtilisateurs(){
        return utilisateurRepository.findAll();
    }
    @PostMapping("users/register")
    public ResponseEntity<?> registerUtilisateur(@RequestBody Utilisateur utilisateur){
        Utilisateur user = utilisateurRepository.save(utilisateur);
        return ResponseEntity.ok(user);
    }
}
