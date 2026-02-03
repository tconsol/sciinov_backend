package com.subbarao.backend.service;

import com.subbarao.backend.entity.DashboardMaster;
import com.subbarao.backend.repository.DashboardMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DashboardMasterService {
    @Autowired
    private DashboardMasterRepository dashboardMasterRepository;

    public List<DashboardMaster> getAllDashboardMasters() {
        return dashboardMasterRepository.findByDeletedFalse();
    }

    public Optional<DashboardMaster> getDashboardMasterById(String id) {
        return dashboardMasterRepository.findByIdAndDeletedFalse(id);
    }

    public DashboardMaster createDashboardMaster(DashboardMaster dashboardMaster) {
        dashboardMaster.setCreatedAt(LocalDateTime.now());
        dashboardMaster.setUpdatedAt(LocalDateTime.now());
        return dashboardMasterRepository.save(dashboardMaster);
    }

    public DashboardMaster updateDashboardMaster(String id, DashboardMaster dashboardMasterDetails) {
        DashboardMaster dashboardMaster = dashboardMasterRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("DashboardMaster not found"));

        dashboardMaster.setName(dashboardMasterDetails.getName());
        dashboardMaster.setStatus(dashboardMasterDetails.isStatus());
        dashboardMaster.setUpdatedAt(LocalDateTime.now());

        return dashboardMasterRepository.save(dashboardMaster);
    }

    public void deleteDashboardMaster(String id) {
        DashboardMaster dashboardMaster = dashboardMasterRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("DashboardMaster not found"));
        dashboardMaster.setDeleted(true);
        dashboardMaster.setUpdatedAt(LocalDateTime.now());
        dashboardMasterRepository.save(dashboardMaster);
    }
}
