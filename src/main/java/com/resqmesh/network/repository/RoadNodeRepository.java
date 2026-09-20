package com.resqmesh.network.repository;

import com.resqmesh.network.entity.RoadNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoadNodeRepository extends JpaRepository<RoadNode, String> {
}
