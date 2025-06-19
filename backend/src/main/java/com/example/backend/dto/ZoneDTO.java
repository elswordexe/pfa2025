package com.example.backend.dto;

import java.util.List;
import lombok.Data;

@Data
public class ZoneDTO {
    private Long id;
    private String name;
    private String description;
    private List<ZoneProduitDTO> zoneProduits;
}