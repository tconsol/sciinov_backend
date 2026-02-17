package com.sciinov.dbms.repository;

import com.sciinov.dbms.entity.DashboardData;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardDataRepository extends MongoRepository<DashboardData, String> {
    Optional<DashboardData> findByConferenceIdAndDashboardMasterIdAndEmail(String conferenceId, String dashboardMasterId, String email);
    
    List<DashboardData> findByConferenceIdAndDashboardMasterIdAndSerialNoBetween(
            String conferenceId, String dashboardMasterId, Long fromSerialNo, Long toSerialNo, Sort sort);
            
    long countByConferenceIdAndDashboardMasterId(String conferenceId, String dashboardMasterId);
    
    Optional<DashboardData> findTopByConferenceIdAndDashboardMasterIdOrderBySerialNoDesc(String conferenceId, String dashboardMasterId);

    // Date-specific queries - filter by upload date range
    List<DashboardData> findByConferenceIdAndDashboardMasterIdAndCreatedAtBetween(
            String conferenceId, String dashboardMasterId, LocalDateTime startDate, LocalDateTime endDate, Sort sort);

    // Region-specific queries
    List<DashboardData> findByConferenceIdAndDashboardMasterIdAndRegionIgnoreCase(
            String conferenceId, String dashboardMasterId, String region, Sort sort);

    // Country-specific queries
    List<DashboardData> findByConferenceIdAndDashboardMasterIdAndCountryIgnoreCase(
            String conferenceId, String dashboardMasterId, String country, Sort sort);

    // Combined: Date + Region filter
    @Query("{'conferenceId': ?0, 'dashboardMasterId': ?1, 'createdAt': {$gte: ?2, $lte: ?3}, 'region': {$regex: ?4, $options: 'i'}}")
    List<DashboardData> findByConferenceAndDateRangeAndRegion(
            String conferenceId, String dashboardMasterId, LocalDateTime startDate, LocalDateTime endDate, String region, Sort sort);

    // Combined: Date + Country filter
    @Query("{'conferenceId': ?0, 'dashboardMasterId': ?1, 'createdAt': {$gte: ?2, $lte: ?3}, 'country': {$regex: ?4, $options: 'i'}}")
    List<DashboardData> findByConferenceAndDateRangeAndCountry(
            String conferenceId, String dashboardMasterId, LocalDateTime startDate, LocalDateTime endDate, String country, Sort sort);

    // Serial number range + Date filter
    @Query("{'conferenceId': ?0, 'dashboardMasterId': ?1, 'serialNo': {$gte: ?2, $lte: ?3}, 'createdAt': {$gte: ?4, $lte: ?5}}")
    List<DashboardData> findByConferenceAndSerialRangeAndDateRange(
            String conferenceId, String dashboardMasterId, Long fromSerialNo, Long toSerialNo,
            LocalDateTime startDate, LocalDateTime endDate, Sort sort);

    // Serial number range + Region filter
    @Query("{'conferenceId': ?0, 'dashboardMasterId': ?1, 'serialNo': {$gte: ?2, $lte: ?3}, 'region': {$regex: ?4, $options: 'i'}}")
    List<DashboardData> findByConferenceAndSerialRangeAndRegion(
            String conferenceId, String dashboardMasterId, Long fromSerialNo, Long toSerialNo, String region, Sort sort);

    // Serial number range + Country filter
    @Query("{'conferenceId': ?0, 'dashboardMasterId': ?1, 'serialNo': {$gte: ?2, $lte: ?3}, 'country': {$regex: ?4, $options: 'i'}}")
    List<DashboardData> findByConferenceAndSerialRangeAndCountry(
            String conferenceId, String dashboardMasterId, Long fromSerialNo, Long toSerialNo, String country, Sort sort);

    // Get distinct regions for a conference/dashboard
    @Query(value = "{'conferenceId': ?0, 'dashboardMasterId': ?1}", fields = "{'region': 1}")
    List<DashboardData> findDistinctRegionsByConferenceAndDashboard(String conferenceId, String dashboardMasterId);

    // Get distinct countries for a conference/dashboard
    @Query(value = "{'conferenceId': ?0, 'dashboardMasterId': ?1}", fields = "{'country': 1}")
    List<DashboardData> findDistinctCountriesByConferenceAndDashboard(String conferenceId, String dashboardMasterId);
}
