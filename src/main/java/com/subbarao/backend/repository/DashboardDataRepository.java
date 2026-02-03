package com.subbarao.backend.repository;

import com.subbarao.backend.entity.DashboardData;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardDataRepository extends MongoRepository<DashboardData, String> {
    Optional<DashboardData> findByConferenceIdAndDashboardMasterIdAndEmail(String conferenceId, String dashboardMasterId, String email);
    
    List<DashboardData> findByConferenceIdAndDashboardMasterIdAndSerialNoBetween(
            String conferenceId, String dashboardMasterId, Long fromSerialNo, Long toSerialNo, Sort sort);
            
    long countByConferenceIdAndDashboardMasterId(String conferenceId, String dashboardMasterId);
    
    Optional<DashboardData> findTopByConferenceIdAndDashboardMasterIdOrderBySerialNoDesc(String conferenceId, String dashboardMasterId);
}
