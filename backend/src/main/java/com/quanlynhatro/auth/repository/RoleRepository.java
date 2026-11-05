package com.quanlynhatro.auth.repository;

import com.quanlynhatro.auth.domain.RoleEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Byte> {
    Optional<RoleEntity> findByCode(String code);
}
