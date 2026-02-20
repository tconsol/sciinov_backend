package com.sciinov.dbms.repository;

import com.sciinov.dbms.entity.ConferenceDocumentLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConferenceDocumentLogRepository extends MongoRepository<ConferenceDocumentLog, String> {

    // Admin's own logs — all conferences, latest first
    List<ConferenceDocumentLog> findByAdminIdOrderByCreatedAtDesc(String adminId);

    // Admin's own logs — scoped to one conference, latest first
    List<ConferenceDocumentLog> findByAdminIdAndConferenceIdOrderByCreatedAtDesc(String adminId, String conferenceId);

    // All logs for a specific conference (super admin), latest first
    List<ConferenceDocumentLog> findByConferenceIdOrderByCreatedAtDesc(String conferenceId);

    // All logs, latest first (super admin)
    List<ConferenceDocumentLog> findAllByOrderByCreatedAtDesc();

    // All logs for a specific admin (super admin view), latest first
    List<ConferenceDocumentLog> findByAdminIdAndActionTypeOrderByCreatedAtDesc(String adminId, ConferenceDocumentLog.ActionType actionType);

    // Filter by action type, latest first
    List<ConferenceDocumentLog> findByActionTypeOrderByCreatedAtDesc(ConferenceDocumentLog.ActionType actionType);
}

