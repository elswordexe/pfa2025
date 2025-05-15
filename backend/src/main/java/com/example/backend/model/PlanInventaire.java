package com.example.backend.model;

import java.util.Date;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.*;
import java.util.HashMap;


@Entity
@Table(name = "plan_inventaires")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PlanInventaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    @Column(nullable = false)
    private String nom;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date datedebut;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date datefin;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private STATUS status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TYPE type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RECCURENCE reccurence;
    
    @ElementCollection
    @CollectionTable(
        name = "quantites_comptees",
        joinColumns = @JoinColumn(name = "plan_id", nullable = false)
    )
    @MapKeyJoinColumn(name = "zone_id")
    @Column(name = "quantite", nullable = false)
    private Map<Zone, Integer> quantitecomptee = new HashMap<>();
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(
        name = "plan_ecarts",
        joinColumns = @JoinColumn(name = "plan_id"),
        inverseJoinColumns = @JoinColumn(name = "ecart_id")
    )
    @MapKeyJoinColumn(name = "zone_id")
    private Map<Zone, Ecart> ecarts = new HashMap<>();
}