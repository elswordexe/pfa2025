package com.example.backend.repository;

import com.example.backend.model.ZoneProduit;
import com.example.backend.model.ZoneProduitId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ZoneProduitRepository extends JpaRepository<ZoneProduit, ZoneProduitId> {

}
