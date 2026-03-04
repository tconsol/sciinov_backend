package com.sciinov.dbms.controller;

import com.sciinov.dbms.entity.DashboardMaster;
import com.sciinov.dbms.service.DashboardMasterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard-masters")
public class DashboardMasterController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardMasterController.class);

    @Autowired
    private DashboardMasterService dashboardMasterService;

    /**
     * Get all dashboard types (active + inactive)
     * SUPER_ADMIN and ADMIN can access
     */
    @GetMapping("/types")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getDashboardTypes() {
        logger.info("GET /api/dashboard-masters/types - Retrieving all dashboard types");
        List<DashboardMaster> types = dashboardMasterService.getAllDashboardMasters();
        logger.info("GET /api/dashboard-masters/types - Retrieved {} dashboard types", types.size());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "total", types.size(),
                "data", types
        ));
    }

    /**
     * Get all dashboard masters
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public List<DashboardMaster> getAllDashboardMasters() {
        logger.info("GET /api/dashboard-masters - Retrieving all dashboard masters");
        List<DashboardMaster> masters = dashboardMasterService.getAllDashboardMasters();
        logger.info("GET /api/dashboard-masters - Retrieved {} dashboard masters", masters.size());
        return masters;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<DashboardMaster> getDashboardMasterById(@PathVariable String id) {
        logger.info("GET /api/dashboard-masters/{} - Retrieving dashboard master by ID", id);
        return dashboardMasterService.getDashboardMasterById(id)
                .map(master -> {
                    logger.info("GET /api/dashboard-masters/{} - Dashboard master found: {}", id, master.getName());
                    return ResponseEntity.ok(master);
                })
                .orElseGet(() -> {
                    logger.warn("GET /api/dashboard-masters/{} - Dashboard master not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public DashboardMaster createDashboardMaster(@RequestBody DashboardMaster dashboardMaster) {
        logger.info("POST /api/dashboard-masters - Creating new dashboard master: {}", dashboardMaster.getName());
        DashboardMaster created = dashboardMasterService.createDashboardMaster(dashboardMaster);
        logger.info("POST /api/dashboard-masters - Dashboard master created successfully with ID: {}", created.getId());
        return created;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<DashboardMaster> updateDashboardMaster(@PathVariable String id, @RequestBody DashboardMaster dashboardMaster) {
        logger.info("PUT /api/dashboard-masters/{} - Updating dashboard master: {}", id, dashboardMaster.getName());
        DashboardMaster updated = dashboardMasterService.updateDashboardMaster(id, dashboardMaster);
        logger.info("PUT /api/dashboard-masters/{} - Dashboard master updated successfully", id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteDashboardMaster(@PathVariable String id) {
        logger.info("DELETE /api/dashboard-masters/{} - Deleting dashboard master", id);
        dashboardMasterService.deleteDashboardMaster(id);
        logger.info("DELETE /api/dashboard-masters/{} - Dashboard master deleted successfully", id);
        return ResponseEntity.ok().build();
    }
}
