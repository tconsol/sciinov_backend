package com.subbarao.backend.controller;

import com.subbarao.backend.entity.DashboardMaster;
import com.subbarao.backend.service.DashboardMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/dashboard-masters")
public class DashboardMasterController {
    @Autowired
    private DashboardMasterService dashboardMasterService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public List<DashboardMaster> getAllDashboardMasters() {
        return dashboardMasterService.getAllDashboardMasters();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<DashboardMaster> getDashboardMasterById(@PathVariable String id) {
        return dashboardMasterService.getDashboardMasterById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public DashboardMaster createDashboardMaster(@RequestBody DashboardMaster dashboardMaster) {
        return dashboardMasterService.createDashboardMaster(dashboardMaster);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<DashboardMaster> updateDashboardMaster(@PathVariable String id, @RequestBody DashboardMaster dashboardMaster) {
        return ResponseEntity.ok(dashboardMasterService.updateDashboardMaster(id, dashboardMaster));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteDashboardMaster(@PathVariable String id) {
        dashboardMasterService.deleteDashboardMaster(id);
        return ResponseEntity.ok().build();
    }
}
