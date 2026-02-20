package com.sciinov.dbms.repository;

import com.sciinov.dbms.entity.DocumentTypeEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentTypeRepository extends MongoRepository<DocumentTypeEntity, String> {

    Optional<DocumentTypeEntity> findBySlugAndDeletedFalse(String slug);

    Optional<DocumentTypeEntity> findByDisplayNameIgnoreCaseAndDeletedFalse(String displayName);

    List<DocumentTypeEntity> findByDeletedFalseOrderBySortOrderAscDisplayNameAsc();

    List<DocumentTypeEntity> findByActiveAndDeletedFalseOrderBySortOrderAscDisplayNameAsc(boolean active);

    boolean existsBySlugAndDeletedFalse(String slug);

    boolean existsByDisplayNameIgnoreCaseAndDeletedFalse(String displayName);
}

