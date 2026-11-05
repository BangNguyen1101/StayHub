package com.quanlynhatro.maintenance.service;

import com.quanlynhatro.maintenance.api.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MaintenanceService {
    MaintenanceResponse createForTenant(MaintenanceCreateRequest request, Long tenantId);

    Page<MaintenanceResponse> searchForTenant(
            MaintenanceQuery query, Long tenantId, Pageable pageable);

    MaintenanceResponse getForTenant(Long id, Long tenantId);

    Page<MaintenanceResponse> searchForAdmin(
            MaintenanceQuery query, Long adminId, Pageable pageable);

    MaintenanceResponse getForAdmin(Long id, Long adminId);

    MaintenanceResponse updateForAdmin(
            Long id, MaintenanceAdminUpdateRequest request, Long adminId);

    List<MaintenanceHistoryResponse> historyForAdmin(Long id, Long adminId);

    List<MaintenanceAttachmentResponse> uploadImages(
            Long id, List<MultipartFile> files, Long tenantId);

    MaintenanceFile downloadForTenant(Long attachmentId, Long tenantId);

    MaintenanceFile downloadForAdmin(Long attachmentId, Long adminId);

    record MaintenanceFile(String originalFileName, String contentType, java.nio.file.Path path) {}
}
