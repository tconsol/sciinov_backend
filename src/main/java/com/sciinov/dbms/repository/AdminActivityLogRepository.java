package com.sciinov.dbms.repository;

import com.sciinov.dbms.entity.AdminActivityLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminActivityLogRepository extends MongoRepository<AdminActivityLog, String> {
    List<AdminActivityLog> findByAdminId(String adminId);
    List<AdminActivityLog> findByConferenceId(String conferenceId);
}
