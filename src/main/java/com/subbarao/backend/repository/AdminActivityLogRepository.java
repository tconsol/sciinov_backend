package com.subbarao.backend.repository;

import com.subbarao.backend.entity.AdminActivityLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminActivityLogRepository extends MongoRepository<AdminActivityLog, String> {
    List<AdminActivityLog> findByAdminId(String adminId);
    List<AdminActivityLog> findByConferenceId(String conferenceId);
}
