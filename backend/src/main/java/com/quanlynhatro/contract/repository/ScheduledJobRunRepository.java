package com.quanlynhatro.contract.repository;

import com.quanlynhatro.contract.domain.ScheduledJobRunEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduledJobRunRepository extends JpaRepository<ScheduledJobRunEntity, Long> {}
