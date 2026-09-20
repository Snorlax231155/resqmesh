package com.resqmesh.network.repository;

import com.resqmesh.network.entity.RoadEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoadEdgeRepository extends JpaRepository<RoadEdge, String> {
    List<RoadEdge> findByBlockedFalse();
}
