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
     * Find document by conference, year, and type
     */
    Optional<ConferenceDocument> findByConferenceIdAndYearAndDocumentTypeAndDeletedFalse(
            String conferenceId, Integer year, ConferenceDocument.DocumentType documentType);

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
     * Find documents for a specific conference and document type
     */
    List<ConferenceDocument> findByConferenceIdAndDocumentTypeAndDeletedFalseOrderByYearDesc(
            String conferenceId, ConferenceDocument.DocumentType documentType);

    /**
     * Find documents of a specific type across all conferences
     */
    List<ConferenceDocument> findByDocumentTypeAndDeletedFalseOrderByConferenceNameAscYearDesc(
            ConferenceDocument.DocumentType documentType);

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
     * Check if document exists for given conference, year, and type
     */
    boolean existsByConferenceIdAndYearAndDocumentTypeAndDeletedFalse(
            String conferenceId, Integer year, ConferenceDocument.DocumentType documentType);

    /**
     * Count documents for a conference
     */
    long countByConferenceIdAndDeletedFalse(String conferenceId);

    /**
     * Count documents for a specific year
     */
    long countByYearAndDeletedFalse(Integer year);
}

