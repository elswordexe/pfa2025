package com.example.backend.model;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ZoneProduitId implements Serializable {
    private Long zoneId;
    private Long produitId;
}