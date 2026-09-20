package com.resqmesh.disruption.repository;

import com.resqmesh.disruption.entity.Disruption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisruptionRepository extends JpaRepository<Disruption, UUID> {
    List<Disruption> findByActiveTrue();
}
