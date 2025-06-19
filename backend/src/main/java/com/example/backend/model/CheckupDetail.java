package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "checkup_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckupDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id")
    @JsonIgnoreProperties({"category", "subCategory", "checkupDetails", "hibernateLazyInitializer"})
    private Produit produit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"details", "hibernateLazyInitializer"})
    private Checkup checkup;

    private Integer scannedQuantity;
    private Integer manualQuantity;
}
