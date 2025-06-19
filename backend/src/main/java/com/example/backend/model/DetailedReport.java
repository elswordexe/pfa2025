package com.example.backend.model;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity
@Table(name = "detailed_report")
public class DetailedReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private LocalDate date;
    private String zone;
    private String type;
    private Integer quantity;
    private String status;
    
    // Getters, setters, constructors
}