package com.quanlynhatro.utility.repository;

import com.quanlynhatro.utility.domain.ServiceEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    boolean existsByServiceCodeIgnoreCase(String serviceCode);

    @Query(
            """
            select s from ServiceEntity s
            where (:keyword is null or :keyword = ''
              or lower(s.serviceCode) like lower(concat('%', :keyword, '%'))
              or lower(s.serviceName) like lower(concat('%', :keyword, '%')))
              and (:pricingMode is null or s.pricingMode = :pricingMode)
              and (:active is null or s.active = :active)
            """)
    Page<ServiceEntity> search(
            @Param("keyword") String keyword,
            @Param("pricingMode") String pricingMode,
            @Param("active") Boolean active,
            Pageable pageable);
}
