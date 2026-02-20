package com.sciinov.dbms.repository;

import com.sciinov.dbms.entity.Conference;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConferenceRepository extends MongoRepository<Conference, String> {
    List<Conference> findByDeletedFalse();
    Optional<Conference> findByIdAndDeletedFalse(String id);
}
