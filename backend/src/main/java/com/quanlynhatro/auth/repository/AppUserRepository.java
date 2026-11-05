package com.quanlynhatro.auth.repository;

import com.quanlynhatro.auth.domain.AppUserEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {
    @EntityGraph(attributePaths = "role")
    Optional<AppUserEntity> findByUsernameIgnoreCaseOrEmailIgnoreCase(
            String username, String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Optional<AppUserEntity> findByIdAndRole_Code(Long id, String roleCode);

    Optional<AppUserEntity> findFirstByRole_CodeAndActiveTrueOrderByIdAsc(String roleCode);

    List<AppUserEntity> findAllByRole_CodeAndActiveTrue(String roleCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUserEntity u where u.id = :id")
    Optional<AppUserEntity> findByIdForUpdate(@Param("id") Long id);

    @Query(
            """
            select u from AppUserEntity u
            where u.role.code = 'TENANT'
              and (:active is null or u.active = :active)
              and (:keyword is null or :keyword = ''
                   or lower(u.username) like lower(concat('%', :keyword, '%'))
                   or lower(u.email) like lower(concat('%', :keyword, '%'))
                   or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                   or lower(u.phoneNumber) like lower(concat('%', :keyword, '%')))
            """)
    Page<AppUserEntity> searchTenants(
            @Param("keyword") String keyword, @Param("active") Boolean active, Pageable pageable);
}
