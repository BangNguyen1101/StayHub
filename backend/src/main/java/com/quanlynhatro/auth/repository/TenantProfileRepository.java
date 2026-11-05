package com.quanlynhatro.auth.repository;

import com.quanlynhatro.auth.domain.TenantProfileEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TenantProfileRepository extends JpaRepository<TenantProfileEntity, Long> {
    Optional<TenantProfileEntity> findByUserId(Long userId);

    Optional<TenantProfileEntity> findByUserIdAndUser_Role_Code(Long userId, String roleCode);

    @Query(
            """
            select p from TenantProfileEntity p
            join p.user u
            where u.role.code = 'TENANT'
              and (:active is null or u.active = :active)
              and (:keyword is null or :keyword = ''
                   or lower(u.username) like lower(concat('%', :keyword, '%'))
                   or lower(u.email) like lower(concat('%', :keyword, '%'))
                   or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                   or lower(u.phoneNumber) like lower(concat('%', :keyword, '%'))
                   or lower(p.contactAddress) like lower(concat('%', :keyword, '%'))
                   or lower(p.identityDocumentNumber) like lower(concat('%', :keyword, '%')))
            """)
    Page<TenantProfileEntity> searchTenants(
            @Param("keyword") String keyword, @Param("active") Boolean active, Pageable pageable);
}
