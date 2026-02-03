package com.subbarao.backend.repository;

import com.subbarao.backend.entity.DashboardMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardMasterRepository extends MongoRepository<DashboardMaster, String> {
    List<DashboardMaster> findByDeletedFalse();
    Optional<DashboardMaster> findByIdAndDeletedFalse(String id);
}
