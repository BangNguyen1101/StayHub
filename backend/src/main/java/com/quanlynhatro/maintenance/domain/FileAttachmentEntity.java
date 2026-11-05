package com.quanlynhatro.maintenance.domain;

import com.quanlynhatro.auth.domain.AppUserEntity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "FileAttachments", schema = "dbo")
public class FileAttachmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FileAttachmentId", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaintenanceRequestId", nullable = false)
    private MaintenanceRequestEntity request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UploadedByUserId", nullable = false)
    private AppUserEntity uploadedBy;

    @Column(name = "OriginalFileName", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "StoredFileName", nullable = false, length = 255)
    private String storedFileName;

    @Column(name = "StoragePath", nullable = false, length = 1000)
    private String storagePath;

    @Column(name = "ContentType", nullable = false, length = 150)
    private String contentType;

    @Column(name = "FileSizeBytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "FileHash", length = 128)
    private String fileHash;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    protected FileAttachmentEntity() {}

    public static FileAttachmentEntity create(
            MaintenanceRequestEntity request,
            AppUserEntity uploadedBy,
            String originalFileName,
            String storedFileName,
            String storagePath,
            String contentType,
            long fileSizeBytes,
            String fileHash) {
        FileAttachmentEntity entity = new FileAttachmentEntity();
        entity.request = request;
        entity.uploadedBy = uploadedBy;
        entity.originalFileName = originalFileName;
        entity.storedFileName = storedFileName;
        entity.storagePath = storagePath;
        entity.contentType = contentType;
        entity.fileSizeBytes = fileSizeBytes;
        entity.fileHash = fileHash;
        entity.createdAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
        return entity;
    }

    public Long getId() {
        return id;
    }

    public MaintenanceRequestEntity getRequest() {
        return request;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
