package com.example.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "assignations_agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignationAgent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanInventaire planInventaire;

    @ManyToOne
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentInventaire agent;

    @ManyToOne
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @ManyToOne
    @JoinColumn(name = "assigne_par", nullable = false)
    private SuperAdministrateur assignePar;

    @Column(nullable = false)
    private LocalDateTime dateAssignation;
}