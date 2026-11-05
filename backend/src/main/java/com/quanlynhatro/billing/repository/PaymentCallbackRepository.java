package com.quanlynhatro.billing.repository;

import com.quanlynhatro.billing.domain.PaymentCallbackEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCallbackRepository extends JpaRepository<PaymentCallbackEntity, Long> {}
