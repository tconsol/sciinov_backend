package com.subbarao.backend.repository;

import com.subbarao.backend.entity.DashboardUploadStats;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardUploadStatsRepository extends MongoRepository<DashboardUploadStats, String> {
    List<DashboardUploadStats> findByAdminId(String adminId);
    List<DashboardUploadStats> findByConferenceId(String conferenceId);
}
