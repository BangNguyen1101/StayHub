package com.quanlynhatro.maintenance.repository;

import com.quanlynhatro.maintenance.domain.FileAttachmentEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileAttachmentRepository extends JpaRepository<FileAttachmentEntity, Long> {
    List<FileAttachmentEntity> findByRequest_IdOrderByCreatedAtAsc(Long requestId);
}
