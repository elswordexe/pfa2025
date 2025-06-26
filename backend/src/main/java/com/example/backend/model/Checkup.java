package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "checkups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Checkup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    @JsonIgnoreProperties({"assignations", "checkups", "hibernateLazyInitializer"})
    private AgentInventaire agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    @JsonIgnoreProperties({"assignations", "checkups", "hibernateLazyInitializer"})
    private PlanInventaire plan;

    @OneToMany(mappedBy = "checkup", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("checkup")
    private List<CheckupDetail> details = new ArrayList<>();

    private LocalDateTime dateCheck;
    
    private boolean valide;
    
    private boolean demandeRecomptage;
    
    private String justificationRecomptage;

}