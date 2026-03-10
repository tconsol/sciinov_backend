package com.sciinov.dbms.repository;

import com.sciinov.dbms.entity.ConferenceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConferenceDocumentRepository extends MongoRepository<ConferenceDocument, String> {

    /**
     * Find document by conference, year, and type slug — returns first match (for single-doc lookups)
     */
    Optional<ConferenceDocument> findByConferenceIdAndYearAndDocumentTypeAndDeletedFalse(
            String conferenceId, Integer year, String documentType);

    /**
     * Find ALL documents by conference, year, and type slug — supports multiple files per type/year
     */
    List<ConferenceDocument> findAllByConferenceIdAndYearAndDocumentTypeAndDeletedFalse(
            String conferenceId, Integer year, String documentType);

    /**
     * Find all documents for a specific conference
     */
    List<ConferenceDocument> findByConferenceIdAndDeletedFalseOrderByYearDescUpdatedAtDesc(String conferenceId);

    /**
     * Find all documents for a specific year (across all conferences)
     */
    List<ConferenceDocument> findByYearAndDeletedFalseOrderByConferenceNameAscUpdatedAtDesc(Integer year);

    /**
     * Find documents for a specific conference and year
     */
    List<ConferenceDocument> findByConferenceIdAndYearAndDeletedFalseOrderByDocumentTypeAsc(String conferenceId, Integer year);

    /**
     * Find documents for a specific conference and document type slug
     */
    List<ConferenceDocument> findByConferenceIdAndDocumentTypeAndDeletedFalseOrderByYearDesc(
            String conferenceId, String documentType);

    /**
     * Find documents of a specific type slug across all conferences
     */
    List<ConferenceDocument> findByDocumentTypeAndDeletedFalseOrderByConferenceNameAscYearDesc(String documentType);

    /**
     * Find all years available for a conference
     */
    @Query(value = "{ 'conferenceId': ?0, 'deleted': false }", fields = "{ 'year': 1 }")
    List<ConferenceDocument> findYearsByConferenceId(String conferenceId);

    /**
     * Find documents by conference name
     */
    List<ConferenceDocument> findByConferenceNameAndDeletedFalseOrderByYearDescUpdatedAtDesc(String conferenceName);

    /**
     * Check if document exists for given conference, year, and type slug
     */
    boolean existsByConferenceIdAndYearAndDocumentTypeAndDeletedFalse(
            String conferenceId, Integer year, String documentType);

    /**
     * Count documents for a conference
     */
    long countByConferenceIdAndDeletedFalse(String conferenceId);

    /**
     * Count documents for a specific year
     */
    long countByYearAndDeletedFalse(Integer year);

    /**
     * Count documents by conference and type slug
     */
    long countByConferenceIdAndDocumentTypeAndDeletedFalse(String conferenceId, String documentType);

    /**
     * Find all non-deleted documents across all conferences, ordered by updated at desc
     */
    List<ConferenceDocument> findByDeletedFalseOrderByUpdatedAtDesc();

    /**
     * Find all documents (deleted and non-deleted)
     */
    List<ConferenceDocument> findByDeletedFalse();

    /**
     * Find all documents for a list of conferenceIds
     */
    List<ConferenceDocument> findByConferenceIdInAndDeletedFalseOrderByYearDescUpdatedAtDesc(List<String> conferenceIds);
}
